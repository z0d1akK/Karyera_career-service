package com.zodiakk.careerservice.career.repository;

import com.zodiakk.careerservice.career.entity.CareerTrack;
import com.zodiakk.careerservice.common.enums.*;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.*;

@Repository
public interface CareerTrackRepository extends JpaRepository<CareerTrack, UUID>, JpaSpecificationExecutor<CareerTrack> {

    Optional<CareerTrack> findByCodeIgnoreCase(String code);

    boolean existsByCodeIgnoreCase(String code);

    boolean existsByCodeIgnoreCaseAndIdNot(String code, UUID id);

    List<CareerTrack> findAllByStatus(CareerTrackStatus status);

    List<CareerTrack> findAllByType(CareerTrackType type);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT t FROM CareerTrack t WHERE t.id = :trackId")
    Optional<CareerTrack> findByIdForUpdate(@Param("trackId") UUID trackId);

}
