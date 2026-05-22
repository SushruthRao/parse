package com.project.rxparser.service.impl;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

import com.project.rxparser.dto.*;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.project.rxparser.exception.InvalidBundleKeyException;
import com.project.rxparser.exception.InvalidFileException;
import com.project.rxparser.model.MembershipInfo;
import com.project.rxparser.model.RxInfo;
import com.project.rxparser.repository.MembershipInfoRepository;
import com.project.rxparser.repository.RxInfoRepository;
import com.project.rxparser.service.RxService;

import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

@Service
@Slf4j
public class RxServiceImpl implements RxService {

	private final MembershipInfoRepository membershipRepository;

	private final ObjectMapper objectMapper;

	private static final Set<String> VALID_BUNDLE_KEYS = Set.of(
			"memberId",
			"lastname",
			"firstname",
			"dob"
	);


	public RxServiceImpl(MembershipInfoRepository membershipInfoRepository,
			ObjectMapper objectMapper) {
		this.membershipRepository = membershipInfoRepository;
		this.objectMapper = objectMapper;
	}

	
	/**
	 * Processes given .txt file and parses json values and saves to database
	 * @param file (Provide a .txt file from controller)
	 * @param bundleKey ( + seperated bundle keys such as firstname+lastname+dob)
	 * @return returns bundled response
	 */
	@Override
	@Transactional
	public BundledAndInvalidRecordsDto processAndUploadFile(MultipartFile file, String bundleKey) {

		 validateFile(file);

		 List<String> bundleKeys = getValidBundleKeys(bundleKey);

		 List<RawJsonDataDto> rawDataList = getRawDataFromFile(file);

		ValidAndInvalidRecordsDto validAndInvalidRecords = getValidAndInvalidRecords(rawDataList);

		Map<List<String>, List<RawJsonDataDto>> groupedData = groupByBundleKey(validAndInvalidRecords.validRecords(), bundleKeys);

		List<RxBundledResponseDto> bundledResponseList = getBundledResponse(groupedData, bundleKeys);

		BundledAndInvalidRecordsDto bundledAndInvalidRecordsDto = new BundledAndInvalidRecordsDto(bundledResponseList, validAndInvalidRecords.invalidRecords());

		saveToDatabase(validAndInvalidRecords.validRecords());

		return bundledAndInvalidRecordsDto;
	}
	
	private void validateFile(MultipartFile file)
	{
		if( file == null || file.isEmpty())
		{
			throw new InvalidFileException("Empty or null file, please upload file");
		}
		String fileName = file.getOriginalFilename().toLowerCase();

		if(!fileName.endsWith(".txt"))
		{
			throw new InvalidFileException("Invalid file, please upload .txt file");
		}
	}

	private List<String> getValidBundleKeys(String bundleKey)
	{
		if (bundleKey == null || bundleKey.isBlank()) {
			throw new InvalidBundleKeyException("Bundle key is empty");
		}
		String[] bundleKeys = bundleKey.split("\\+");

		List<String> invalidBundleKeys = Arrays.stream(bundleKeys)
												.filter(key -> !VALID_BUNDLE_KEYS.contains(key))
				 								.toList();

		if(!invalidBundleKeys.isEmpty())
			throw new InvalidBundleKeyException("Invalid bundle key: " + invalidBundleKeys);

		return Arrays.asList(bundleKeys);
	}

	private List<RawJsonDataDto> getRawDataFromFile(MultipartFile file)
	{
		List<RawJsonDataDto> rawDataList = new ArrayList<>();

		try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream()))) {
			String line;
			while ((line = reader.readLine()) != null) {
				if (line.trim().isEmpty())
					continue;
				rawDataList.add(objectMapper.readValue(line, RawJsonDataDto.class));
			}
		} catch (IOException exception) {

			log.info("[RxServiceImpl.getRawDataFromFile] IOException occurred : {} ", exception.getMessage());
			throw new RuntimeException("Error while reading from .txt file", exception);

		} catch (JacksonException exception) {

			log.info("[RxServiceImpl.getRawDataFromFile] JacksonException occurred : {} ", exception.getMessage());
			throw new RuntimeException("Error while parsing JSON", exception);
		}
		return rawDataList;
	}


	private Map<List<String>, List<RawJsonDataDto>> groupByBundleKey(List<RawJsonDataDto> rawDataList, List<String> bundleKeys) {
		return rawDataList.stream()
				.collect(Collectors.groupingBy(
						record -> bundleKeys.stream()
															.map(key -> checkField(record, key))
															.toList(),
						LinkedHashMap::new,
						Collectors.toList()
				));
	}

	private String checkField(RawJsonDataDto record, String key) {
		return switch (key) {
			case "memberId"  -> record.memberId();
			case "firstname" -> record.firstname();
			case "lastname"  -> record.lastname();
			case "dob"       -> record.dob();
			default -> throw new InvalidBundleKeyException("Unexpected bundle key: " + key);
		};
	}

	private List<RxBundledResponseDto> getBundledResponse(Map<List<String>, List<RawJsonDataDto>> groupedData, List<String> bundleKeys)
	{
		return groupedData.values().stream()
				.map(group -> {
					// Get the member fields from the group
					RawJsonDataDto memberFields = group.get(0);

					// Get the rxInfoList containing rx, drugName, description
					List<RxInfoDto> rxInfoList = group.stream()
							.map(rxEntry -> new RxInfoDto(rxEntry.rx(),rxEntry.drugName(), rxEntry.description() ))
							.toList();

					// Map to RxBundledResponseDto
					return new RxBundledResponseDto(
							bundleKeys.contains("memberId") ? memberFields.memberId() : null,
							bundleKeys.contains("firstname") ? memberFields.firstname() : null,
							bundleKeys.contains("lastname") ? memberFields.lastname() : null,
							bundleKeys.contains("dob") ? memberFields.dob() : null,
							rxInfoList
					);
				})
				.toList();
	}


	private void saveToDatabase(List<RawJsonDataDto> rawDataList) {
		Map<String, List<RawJsonDataDto>> groupedByMemberId = rawDataList.stream()
				.collect(Collectors.groupingBy(RawJsonDataDto::memberId, LinkedHashMap::new, Collectors.toList()));

		groupedByMemberId.forEach((memberId, records) -> {
			MembershipInfo member = getOrCreateMember(records.get(0));

			Set<String> existingRxInfo = member.getRxInfo().stream()
					.map(record -> record.getRx())
					.collect(Collectors.toSet());

			List<RxInfo> newRxList = records.stream()
					.filter(record -> !existingRxInfo.contains(record.rx()))
					.map(record -> getRxInfo(record, member))
					.toList();

			member.getRxInfo().addAll(newRxList);
			membershipRepository.save(member);
		});
	}

	private MembershipInfo getOrCreateMember(RawJsonDataDto record) {
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


	private boolean checkIfValidRecord(RawJsonDataDto record) {
		if (record.memberId() == null || record.memberId().isBlank() ) return false;
		if (record.firstname() == null || record.firstname().isBlank()) return false;
		if (record.lastname() == null || record.lastname().isBlank())  return false;
		if (record.dob() == null || record.dob().isBlank())  return false;
		if (record.rx()  == null || record.rx().isBlank())  return false;
		if (record.drugName() == null || record.drugName().isBlank())  return false;
		if (record.description() == null || record.description().isBlank()) return false;
		return true;
	}

	private ValidAndInvalidRecordsDto getValidAndInvalidRecords(List<RawJsonDataDto> rawDataList) {
		List<RawJsonDataDto> validList   = new ArrayList<>();
		List<String>         invalidList = new ArrayList<>();

		for (RawJsonDataDto record : rawDataList) {
			if (checkIfValidRecord(record)) validList.add(record);
			else
				invalidList.add(record.toString());
		}


		return new ValidAndInvalidRecordsDto(validList, invalidList);
	}
}
