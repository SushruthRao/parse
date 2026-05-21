package com.project.rxparser.service;

import java.util.List;
import java.util.Map;

import org.springframework.web.multipart.MultipartFile;

import com.project.rxparser.dto.RxBundledResponseDto;

public interface RxService {
	
	public  List<RxBundledResponseDto> processAndUploadFile(MultipartFile file, String bundleKey) ;

}
