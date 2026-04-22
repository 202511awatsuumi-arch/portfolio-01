package com.example.demo.repository;

import com.example.demo.entity.ContactInquiry;
import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ContactInquiryRepository extends JpaRepository<ContactInquiry, Long> {

    Page<ContactInquiry> findAllByDeletedAtIsNullOrderByCreatedAtDesc(Pageable pageable);

    Page<ContactInquiry> findAllByDeletedAtIsNotNullOrderByDeletedAtDescCreatedAtDesc(Pageable pageable);

    Optional<ContactInquiry> findByIdAndDeletedAtIsNull(Long id);

    Optional<ContactInquiry> findByIdAndDeletedAtIsNotNull(Long id);

    @Query("""
            select count(c) from ContactInquiry c
            where c.deletedAt is null
              and (c.createdAt > :createdAt
               or (c.createdAt = :createdAt and c.id > :id))
            """)
    long countNewerThan(@Param("createdAt") LocalDateTime createdAt, @Param("id") Long id);
}
