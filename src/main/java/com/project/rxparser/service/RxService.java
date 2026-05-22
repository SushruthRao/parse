package com.project.rxparser.service;

import java.util.List;
import java.util.Map;

import com.project.rxparser.dto.BundledAndInvalidRecordsDto;
import org.springframework.web.multipart.MultipartFile;

import com.project.rxparser.dto.RxBundledResponseDto;

public interface RxService {
	
	public BundledAndInvalidRecordsDto processAndUploadFile(MultipartFile file, String bundleKey) ;

}
