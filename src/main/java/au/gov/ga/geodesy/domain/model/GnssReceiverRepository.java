package au.gov.ga.geodesy.domain.model;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface GnssReceiverRepository extends JpaRepository<GnssReceiver, Integer> {

    @Override
    List<GnssReceiver> findAll();
}
