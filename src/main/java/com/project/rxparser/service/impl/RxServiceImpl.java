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

import com.project.rxparser.dto.RawJsonDataDto;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.project.rxparser.dto.RxBundledResponseDto;
import com.project.rxparser.dto.RxInfoDto;

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
	private final RxInfoRepository rxInfoRepository;

	private final ObjectMapper objectMapper;

	private static final Set<String> VALID_BUNDLE_KEYS = Set.of(
			"memberId",
			"lastname",
			"firstname",
			"dob"
	);


	public RxServiceImpl(MembershipInfoRepository membershipInfoRepository, RxInfoRepository rxInfoRepository,
			ObjectMapper objectMapper) {
		this.membershipRepository = membershipInfoRepository;
		this.rxInfoRepository = rxInfoRepository;
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
	public List<RxBundledResponseDto> processAndUploadFile(MultipartFile file, String bundleKey) {
		 validateFile(file);
		 List<String> bundleKeys = getValidBundleKeys(bundleKey);
		 List<RawJsonDataDto> rawDataList = getRawDataFromFile(file);
		Map<List<String>, List<RawJsonDataDto>> groupedData = groupByBundleKey(rawDataList, bundleKeys);
		List<RxBundledResponseDto> bundledResponseList = getBundledResponse(groupedData, bundleKeys);

		return bundledResponseList;
	}
	
	private void validateFile(MultipartFile file)
	{
		if( file == null || file.isEmpty())
		{
			throw new InvalidFileException("Empty or null file, please upload file");
		}
		String fileName = file.getOriginalFilename().toLowerCase();

		if(fileName == null || !fileName.endsWith(".txt"))
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
			default -> "";
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


}
