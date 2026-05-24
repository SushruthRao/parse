package com.project.rxparser.service.impl;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.project.rxparser.dto.BundledAndInvalidRecordsDto;
import com.project.rxparser.dto.RawJsonDataDto;
import com.project.rxparser.dto.RxBundledResponseDto;
import com.project.rxparser.dto.RxInfoDto;
import com.project.rxparser.dto.ValidAndInvalidRecordsDto;
import com.project.rxparser.exception.InvalidBundleKeyException;
import com.project.rxparser.exception.InvalidFileException;
import com.project.rxparser.model.MembershipInfo;
import com.project.rxparser.model.RxInfo;
import com.project.rxparser.repository.MembershipInfoRepository;
import com.project.rxparser.service.RxService;

import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

@Service
@Slf4j
public class RxServiceImpl implements RxService {

	private final MembershipInfoRepository membershipRepository;

	private final ObjectMapper objectMapper = new ObjectMapper();

	private static final Set<String> VALID_BUNDLE_KEYS = Set.of("memberId", "lastname", "firstname", "dob");

	public RxServiceImpl(MembershipInfoRepository membershipInfoRepository) {
		this.membershipRepository = membershipInfoRepository;
	}

	/**
	 * Processes given .txt file and parses json values and saves to database
	 * 
	 * @param file      (Provide a .txt file from controller)
	 * @param bundleKey ( + seperated bundle keys such as firstname+lastname+dob)
	 * @return returns bundled response
	 */
	@Override
	@Transactional
	public BundledAndInvalidRecordsDto processAndUploadFile(MultipartFile file, String bundleKey) {

		validateFile(file);

		List<String> bundleKeys = getValidatedBundleKeys(bundleKey);

		ValidAndInvalidRecordsDto validAndInvalidRecords = parseAndValidateFileRecords(file);

		Map<List<String>, List<RawJsonDataDto>> groupedByBundleKeyData = getGroupedDataByBundleKey(validAndInvalidRecords.validRecords(),bundleKeys);
				
		List<RxBundledResponseDto> bundledResponseList = getBundledResponse(groupedByBundleKeyData, bundleKeys);

		BundledAndInvalidRecordsDto bundledAndInvalidRecordsDto = new BundledAndInvalidRecordsDto(bundledResponseList, validAndInvalidRecords.invalidRecords());

		saveToDatabase(validAndInvalidRecords.validRecords());

		return bundledAndInvalidRecordsDto;
	}

	private void validateFile(MultipartFile file) {
		
		// check if file is null or empty
		if (file == null || file.isEmpty()) {
			
			log.info("[RxServiceImpl.validateFile] Empty/null file exception thrown");
			throw new InvalidFileException("Empty or null file, please upload file");
		}
		
		String fileName = file.getOriginalFilename().toLowerCase();

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
		List<RawJsonDataDto> validList = new ArrayList<>();
		List<String> invalidList = new ArrayList<>();

		try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream()))) {
			String line;
			while ((line = reader.readLine()) != null) {
				String trimmedLine = line.trim();
				if (trimmedLine.isEmpty()) continue;

				try {
					RawJsonDataDto record = objectMapper.readValue(trimmedLine, RawJsonDataDto.class);
					if (isValidRecord(record))
						validList.add(record);
					else
						invalidList.add(record.toString());

				} catch (JacksonException e) {
					log.info("[RxServiceImpl.parseAndValidateFileRecords] Malformed JSON skipped: {}", e.getMessage());
					invalidList.add(trimmedLine);
				}
			}
		} catch (IOException e) {
			log.error("[RxServiceImpl.parseAndValidateFileRecords] Failed to read file: {}", e.getMessage());
			throw new RuntimeException("Error while reading from file", e);
		}

		return new ValidAndInvalidRecordsDto(validList, invalidList);
	}


	private Map<List<String>, List<RawJsonDataDto>> getGroupedDataByBundleKey(List<RawJsonDataDto> rawDataList, List<String> bundleKeys) {
		
		return rawDataList.stream()
				.collect(Collectors.groupingBy(
							record -> bundleKeys.stream()
												.map(key -> checkField(record, key))
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

	private List<RxBundledResponseDto> getBundledResponse(Map<List<String>, List<RawJsonDataDto>> groupedData,
			List<String> bundleKeys) {
		
		return groupedData.values().stream().map(group -> {
			// Get the member fields from the group
			RawJsonDataDto memberFields = group.get(0);

			// Get the rxInfoList containing rx, drugName, description
			List<RxInfoDto> rxInfoList = group.stream()
												.map(rxEntry -> new RxInfoDto(rxEntry.rx(), rxEntry.drugName(), rxEntry.description()))
												.toList();

			// create bundled response with rxinfo list
			return new RxBundledResponseDto(bundleKeys.contains("memberId") ? memberFields.memberId() : null,
											bundleKeys.contains("firstname") ?  memberFields.firstname() : null,
											bundleKeys.contains("lastname") ? memberFields.lastname() : null,
											bundleKeys.contains("dob") ? memberFields.dob() : null, 
											rxInfoList);
												}).toList();
	}

	private void saveToDatabase(List<RawJsonDataDto> rawDataList) {
		
		Map<String, List<RawJsonDataDto>> groupedByMemberId = rawDataList.stream()
														.collect(
															Collectors.groupingBy(record -> record.memberId(), 
																							LinkedHashMap::new, 
																							Collectors.toList()
																)
															);

		groupedByMemberId.forEach((memberId, records) -> {
			
			MembershipInfo member = getExistingMemberOrCreateMember(records.getFirst());

			Set<String> existingRxInfo = member.getRxInfo().stream()
															.map(record -> record.getRx())
															.collect(Collectors.toSet());

			List<RxInfo> newRxList = records.stream()
											.filter(record -> !existingRxInfo.contains(record.rx()))
											.map(record -> getRxInfo(record, member)).toList();

			member.getRxInfo().addAll(newRxList);
			membershipRepository.save(member);
		});
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