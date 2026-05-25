package com.project.rxparser.service;

import org.springframework.web.multipart.MultipartFile;

import com.project.rxparser.dto.BundledAndInvalidRecordsDto;

public interface RxService {
	
	public BundledAndInvalidRecordsDto processAndUploadFile(MultipartFile file, String bundleKey, boolean batchEnabled, Integer batchSize) ;

}
