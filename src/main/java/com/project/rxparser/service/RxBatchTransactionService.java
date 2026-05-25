package com.project.rxparser.service;

import com.project.rxparser.dto.IndexedRecord;
import com.project.rxparser.dto.RawJsonDataDto;
import com.project.rxparser.model.MembershipInfo;
import com.project.rxparser.model.RxInfo;
import com.project.rxparser.repository.MembershipInfoRepository;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class RxBatchTransactionService {

    private final MembershipInfoRepository membershipRepository;
    private final EntityManager entityManager;

    public RxBatchTransactionService(MembershipInfoRepository membershipRepository,
                                     EntityManager entityManager) {
        this.membershipRepository = membershipRepository;
        this.entityManager = entityManager;
    }


    public List<String> saveBatch(List<Map.Entry<String, List<IndexedRecord>>> batch) {

        List<String> failedInBatch = new ArrayList<>();

        for (Map.Entry<String, List<IndexedRecord>> entry : batch) {
            String memberId = entry.getKey();
            List<IndexedRecord> indexedRecords = entry.getValue();

            try {
                saveSingleMember(memberId, indexedRecords);
                entityManager.flush();
                entityManager.clear();
                log.debug("[RxBatchTransactionService.saveBatch] Member {} saved", memberId);

            } catch (Exception e) {
                entityManager.clear();
                indexedRecords.forEach(indexed -> failedInBatch.add("Line " + indexed.lineNumber()));
                log.warn("[RxBatchTransactionService.saveBatch] Member {} failed, exception: {}", memberId, e.getMessage());
            }
        }

        return failedInBatch;
    }

    @Transactional(Transactional.TxType.REQUIRES_NEW)
    public void saveSingleMember(String memberId, List<IndexedRecord> indexedRecords) {

        MembershipInfo member = membershipRepository.findById(Long.parseLong(memberId))
                .orElseGet(() -> createNewMember(indexedRecords.getFirst().data()));

        Set<String> existingRx = member.getRxInfo().stream()
                .map(RxInfo::getRx)
                .collect(Collectors.toCollection(HashSet::new));

        for (IndexedRecord indexed : indexedRecords) {
            String rx = indexed.data().rx();

            if (existingRx.contains(rx)) {
                log.debug("[RxBatchTransactionService.saveSingleMember] Skipping duplicate rx {} for member {}", rx, memberId);
                continue;
            }

            member.getRxInfo().add(getRxInfo(indexed.data(), member));
            existingRx.add(rx);
        }

        if (member.getMemberId() != null && membershipRepository.existsById(member.getMemberId())) {
            entityManager.merge(member);   // existing member
        } else {
            entityManager.persist(member); // new member
        }
    }

    private MembershipInfo createNewMember(RawJsonDataDto data) {
        MembershipInfo m = new MembershipInfo();
        m.setMemberId(Long.parseLong(data.memberId()));
        m.setFirstName(data.firstname());
        m.setLastName(data.lastname());
        m.setDob(data.dob());
        return m;
    }

    private RxInfo getRxInfo(RawJsonDataDto data, MembershipInfo member) {
        RxInfo rxInfo = new RxInfo();
        rxInfo.setRx(data.rx());
        rxInfo.setDrugName(data.drugName());
        rxInfo.setDescription(data.description());
        rxInfo.setMember(member);
        return rxInfo;
    }
}
