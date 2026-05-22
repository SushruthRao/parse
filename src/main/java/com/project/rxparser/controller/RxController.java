package com.project.rxparser.controller;

import java.util.List;
import java.util.Map;

import com.project.rxparser.dto.BundledAndInvalidRecordsDto;
import com.project.rxparser.dto.RxBundledResponseDto;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.project.rxparser.dto.ApiResponse;
import com.project.rxparser.service.impl.RxServiceImpl;

import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/rx")
@Slf4j
public class RxController {

	private final RxServiceImpl rxServiceImpl;

	public RxController(RxServiceImpl rxServiceImpl) {
		this.rxServiceImpl = rxServiceImpl;
	}

	/**
	 * Uploads RxFile taking Multipart file (.txt file) and bundleKey string
	 * 
	 * @param file      (.txt file only)
	 * @param bundleKey
	 * @return
	 */
	@PostMapping("/upload")
	public ResponseEntity<ApiResponse<List<RxBundledResponseDto>>> uploadRxFile
			(@RequestParam("file") MultipartFile file,
			@RequestParam("bundle_key") String bundleKey) {

		// Get bundled list
		BundledAndInvalidRecordsDto bundledList = rxServiceImpl.processAndUploadFile(file, bundleKey);

		String message = bundledList.invalidRecords().isEmpty() ? "File upload success" : "File upload success, " + bundledList.invalidRecords().size() + " invalid records found so skipped saving them to db";
		// Add the bundled response json to the response body
		ApiResponse<List<RxBundledResponseDto>> response = new ApiResponse<>(
				true,
				message,
				bundledList.bundledData()
		);
		return ResponseEntity.ok(response);
	}

}
