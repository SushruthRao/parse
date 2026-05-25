package com.project.rxparser.service.impl;

import com.project.rxparser.dto.*;
import com.project.rxparser.exception.InvalidBundleKeyException;
import com.project.rxparser.exception.InvalidFileException;
import com.project.rxparser.model.MembershipInfo;
import com.project.rxparser.model.RxInfo;
import com.project.rxparser.repository.MembershipInfoRepository;
import com.project.rxparser.service.RxBatchSaverService;
import com.project.rxparser.service.RxService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class RxServiceImpl implements RxService {

	private final MembershipInfoRepository membershipRepository;
	private final RxBatchSaverService batchSaverService;
	private final ObjectMapper objectMapper;

	private static final Set<String> VALID_BUNDLE_KEYS = Set.of("memberId", "lastname", "firstname", "dob");

	public RxServiceImpl(MembershipInfoRepository repo, RxBatchSaverService batchSaverService, ObjectMapper objectMapper) {
		this.membershipRepository = repo;
        this.batchSaverService = batchSaverService;
        this.objectMapper = objectMapper;
	}

	/**
	 * Processes given .txt file and parses json values and saves to database
	 * 
	 * @param file      (Provide a .txt file from controller)
	 * @param bundleKey ( + seperated bundle keys such as firstname+lastname+dob)
	 * @return returns bundled response
	 */
	@Override
	public BundledAndInvalidRecordsDto processAndUploadFile(MultipartFile file, String bundleKey, boolean batchEnabled) {

		validateFile(file);

		List<String> bundleKeys = getValidatedBundleKeys(bundleKey);

		ValidAndInvalidRecordsDto validAndInvalidRecords = parseAndValidateFileRecords(file);

		Map<List<String>, List<IndexedRecord>> groupedByBundleKeyData = getGroupedDataByBundleKey(validAndInvalidRecords.validRecords(),bundleKeys);
				
		List<RxBundledResponseDto> bundledResponseList = getBundledResponse(groupedByBundleKeyData, bundleKeys);

		BundledAndInvalidRecordsDto bundledAndInvalidRecordsDto = new BundledAndInvalidRecordsDto(bundledResponseList, validAndInvalidRecords.invalidRecords());

		List<String> failedRecordsLineNumbers;
		if(batchEnabled) {
			failedRecordsLineNumbers = batchSaverService.batchInsert(validAndInvalidRecords.validRecords());
		}
		else{
			failedRecordsLineNumbers = saveToDatabase(validAndInvalidRecords.validRecords());
		}


		bundledAndInvalidRecordsDto.invalidRecords().addAll(failedRecordsLineNumbers);

		return bundledAndInvalidRecordsDto;
	}

	private void validateFile(MultipartFile file) {
		
		// check if file is null or empty
		if (file == null || file.isEmpty()) {
			log.info("[RxServiceImpl.validateFile] Empty/null file exception thrown");
			throw new InvalidFileException("Empty or null file, please upload file");
		}

		String fileName = file.getOriginalFilename() != null ? file.getOriginalFilename().toLowerCase() : "";

		// check if file type is .txt
		if (!fileName.endsWith(".txt")) {
			log.info("[RxServiceImpl.validateFile] Invalid file type exception");
			throw new InvalidFileException("Invalid file, please upload .txt file");
		}
	}

	private List<String> getValidatedBundleKeys(String bundleKey) {
		
		if (bundleKey == null || bundleKey.isBlank()) {
			log.info("[RxServiceImpl.getValidatedBundleKeys] Invalid bundle key exception thrown due to empty bundleKey string");
			throw new InvalidBundleKeyException("Bundle key is empty");
		}
		
		String[] bundleKeys = bundleKey.split("\\+");

		// if VALID_BUNDLE_KEYS does not contain key then throw InvalidBundleKeyException
		List<String> invalidBundleKeys = Arrays.stream(bundleKeys)
												.filter(key -> !VALID_BUNDLE_KEYS.contains(key))
												.toList();

		if (!invalidBundleKeys.isEmpty())
		{
			log.info("[RxServiceImpl.getValidatedBundleKeys] Invalid bundle key exception thrown");
			throw new InvalidBundleKeyException("Invalid bundle key: " + invalidBundleKeys);
		}

		return Arrays.asList(bundleKeys);
	}

	private ValidAndInvalidRecordsDto parseAndValidateFileRecords(MultipartFile file) {
		List<IndexedRecord> validList = new ArrayList<>();
		List<String> invalidList = new ArrayList<>();
		int lineNumber = 0;

		try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream()))) {
			String line;
			while ((line = reader.readLine()) != null){
				lineNumber++;
				String trimmedLine = line.trim();
				if (trimmedLine.isEmpty()) continue;

				try {
					RawJsonDataDto record = objectMapper.readValue(trimmedLine, RawJsonDataDto.class);
					if (isValidRecord(record))
						validList.add(new IndexedRecord(lineNumber, record));
					else
						invalidList.add("Line " + lineNumber);

				} catch (JacksonException e) {
					log.info("[RxServiceImpl.parseAndValidateFileRecords] Invalid Json format skipped at line {}", lineNumber);
					invalidList.add("Line " + lineNumber);
				}
			}
		} catch (IOException e) {
			log.error("[RxServiceImpl.parseAndValidateFileRecords] Failed to read file: {}", e.getMessage());
			throw new RuntimeException("Error while reading from file", e);
		}

		return new ValidAndInvalidRecordsDto(validList, invalidList);
	}


	private Map<List<String>, List<IndexedRecord>> getGroupedDataByBundleKey(List<IndexedRecord> records, List<String> bundleKeys) {
		
		return records.stream()
				.collect(Collectors.groupingBy(
							record -> bundleKeys.stream()
												.map(key -> checkField(record.data(), key))
												.toList(),
							LinkedHashMap::new,
							Collectors.toList()
						)
					  );
	}

	private String checkField(RawJsonDataDto record, String key) {
		return switch (key) {
		case "memberId" -> record.memberId();
		case "firstname" -> record.firstname();
		case "lastname" -> record.lastname();
		case "dob" -> record.dob();
		default -> throw new InvalidBundleKeyException("Unexpected bundle key: " + key);
		};
	}

	private List<RxBundledResponseDto> getBundledResponse(Map<List<String>, List<IndexedRecord>> groupedData,
			List<String> bundleKeys) {
		
		return groupedData.values().stream().map(group -> {
			// Get the member fields from the group
			RawJsonDataDto memberFields = group.get(0).data();

			// Get the rxInfoList containing rx, drugName, description
			List<RxInfoDto> rxInfoList = group.stream()
												.map(rxEntry -> new RxInfoDto(rxEntry.data().rx(), rxEntry.data().drugName(), rxEntry.data().description()))
												.toList();

			// create bundled response with rxinfo list
			return new RxBundledResponseDto(bundleKeys.contains("memberId") ? memberFields.memberId() : null,
											bundleKeys.contains("firstname") ?  memberFields.firstname() : null,
											bundleKeys.contains("lastname") ? memberFields.lastname() : null,
											bundleKeys.contains("dob") ? memberFields.dob() : null, 
											rxInfoList);
												}).toList();
	}

	public List<String> saveToDatabase(List<IndexedRecord> records) {

		List<String> failedRecords = new ArrayList<>();

		Map<String, List<IndexedRecord>> groupedByMemberId = records.stream()
				.collect(Collectors.groupingBy(
						indexed -> indexed.data().memberId(),
						LinkedHashMap::new,
						Collectors.toList()
				));

		int total = groupedByMemberId.size();
		log.info("[saveToDatabase] Starting — {} saving grouped members without batching", total);

		groupedByMemberId.forEach((memberId, indexedRecords) -> {
			try {
				MembershipInfo member = getExistingMemberOrCreateMember(indexedRecords.getFirst().data());

				Set<String> existingRxInfo = member.getRxInfo().stream()
						.map(rxInfo -> rxInfo.getRx())
						.collect(Collectors.toSet());

				List<RxInfo> newRxList = indexedRecords.stream()
						.filter(indexed -> !existingRxInfo.contains(indexed.data().rx()))
						.map(indexed -> getRxInfo(indexed.data(), member))
						.toList();

				member.getRxInfo().addAll(newRxList);
				membershipRepository.save(member);

			}  catch (DataIntegrityViolationException e) {
			// duplicate record
			log.warn("[saveToDatabase] DataIntegrityViolationException for member {}: {}", memberId, e.getMostSpecificCause().getMessage());
			indexedRecords.forEach(record -> failedRecords.add("Line " + record.lineNumber()));


		} catch (DataAccessException e) {
			log.error("[saveToDatabase] DataAccessException for member {}: {}", memberId, e.getMessage());
			indexedRecords.forEach(record -> failedRecords.add("Line " + record.lineNumber()));
		}

		 catch (Exception e) {
			// catches NumberFormatException from parseLong, and anything else unexpected
			log.error("[RxServiceImpl.saveToDatabase] Failed to save member {}: {}", memberId, e.getMessage());
			indexedRecords.forEach(record -> failedRecords.add("Line " + record.lineNumber()));
		}
		});

		return failedRecords;
	}

	private MembershipInfo getExistingMemberOrCreateMember(RawJsonDataDto record) {
		
		return membershipRepository.findById(Long.parseLong(record.memberId()))
								   .orElseGet(() -> {
											MembershipInfo member = new MembershipInfo();
											member.setMemberId(Long.parseLong(record.memberId()));
											member.setFirstName(record.firstname());
											member.setLastName(record.lastname());
											member.setDob(record.dob());
											return member;
									});
	}

	private RxInfo getRxInfo(RawJsonDataDto record, MembershipInfo member) {
		
		RxInfo rxInfo = new RxInfo();
		rxInfo.setRx(record.rx());
		rxInfo.setDrugName(record.drugName());
		rxInfo.setDescription(record.description());
		rxInfo.setMember(member);
		return rxInfo;
	}



	private boolean isValidRecord(RawJsonDataDto record) {
	    return isNotNullAndNotBlank(record.memberId())
	        && isNotNullAndNotBlank(record.firstname())
	        && isNotNullAndNotBlank(record.lastname())
	        && isNotNullAndNotBlank(record.dob())
	        && isNotNullAndNotBlank(record.rx())
	        && isNotNullAndNotBlank(record.drugName())
	        && isNotNullAndNotBlank(record.description());
	}

	private boolean isNotNullAndNotBlank(String value) {
	    return value != null && !value.isBlank();
	}
}