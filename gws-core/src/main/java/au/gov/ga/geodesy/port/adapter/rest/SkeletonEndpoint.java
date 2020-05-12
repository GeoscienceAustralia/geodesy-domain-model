package au.gov.ga.geodesy.port.adapter.rest;

import au.gov.ga.geodesy.domain.model.SiteResponsibleParty;
import au.gov.ga.geodesy.domain.model.sitelog.GnssAntennaLogItem;
import au.gov.ga.geodesy.domain.model.sitelog.GnssReceiverLogItem;
import au.gov.ga.geodesy.domain.model.sitelog.SiteLog;
import au.gov.ga.geodesy.domain.model.sitelog.SiteLogRepository;
import au.gov.ga.gnss.support.rinex.RinexFileHeader;
import com.vividsolutions.jts.geom.Point;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@RestController
@RequestMapping("/skeletonFiles")
public class SkeletonEndpoint {

    private static final Logger log = LoggerFactory.getLogger(SkeletonEndpoint.class);

    @Autowired
    private SiteLogRepository siteLogs;

    @Transactional(readOnly = true)
    @RequestMapping(
        value = "/{filename:[a-zA-Z0-9_]{4}(?:[0-9]{2}[a-zA-Z]{3})?\\.(?:(?:SKL)|(?:skl))}",
        method = RequestMethod.GET,
        produces = "text/plain")
    @ResponseBody
    public ResponseEntity<String> findByFourCharacterId(
        @PathVariable String filename) throws IOException {

        String fourCharId = filename.substring(0, 4).toUpperCase();

        SiteLog siteLog = siteLogs.findByFourCharacterId(fourCharId);
        if (siteLog == null) {
            log.error("Failed to retrive site log for station " + fourCharId);
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        RinexFileHeader rinexFileHeader = new RinexFileHeader();
        rinexFileHeader.setMarkerName(fourCharId);

        String markerNumber = siteLog.getSiteIdentification().getIersDOMESNumber();
        rinexFileHeader.setMarkerNumber(markerNumber);

        List<SiteResponsibleParty> custodian = siteLog.getSiteMetadataCustodians();
        if (!custodian.isEmpty()) {
            String agency = custodian.get(0).getParty().getOrganisationName().toString();
            // Truncate Organisation name if longer than 40 chars
            // TODO: Add Header comment containing untruncated Organisation Name
            // TODO: Consider doing this in RinexFileHeader class
            if (agency.length() > 40) {
                agency = agency.substring(0, 36) + "...";
            }
            rinexFileHeader.setAgency(agency);
        }

        List<GnssReceiverLogItem> receiverLogItemList = new ArrayList<>(siteLog.getGnssReceivers());
        if (receiverLogItemList.isEmpty()) {
            log.error("No receiver record for station " + fourCharId);
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
        receiverLogItemList.sort(Comparator.comparing(GnssReceiverLogItem::getDateInstalled).reversed());

        GnssReceiverLogItem receiverLogItem = receiverLogItemList.get(0);
        rinexFileHeader.setReceiverSerialNumber(receiverLogItem.getSerialNumber());
        rinexFileHeader.setReceiverType(receiverLogItem.getType());
        rinexFileHeader.setReceiverFirmwareVersion(receiverLogItem.getFirmwareVersion());

        List<GnssAntennaLogItem> antennaLogItemList = new ArrayList<>(siteLog.getGnssAntennas());
        if (antennaLogItemList.isEmpty()) {
            log.error("No antenna record for station " + fourCharId);
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
        antennaLogItemList.sort(Comparator.comparing(GnssAntennaLogItem::getDateInstalled).reversed());

        GnssAntennaLogItem antennaLogItem = antennaLogItemList.get(0);
        rinexFileHeader.setAntennaSerialNumber(antennaLogItem.getSerialNumber());
        rinexFileHeader.setAntennaType(antennaLogItem.getType());

        Point position = siteLog.getSiteLocation().getApproximatePosition().getCartesianPosition();
        rinexFileHeader.getApproxPositionXyz().setLeft(position.getCoordinate().x);
        rinexFileHeader.getApproxPositionXyz().setMiddle(position.getCoordinate().y);
        rinexFileHeader.getApproxPositionXyz().setRight(position.getCoordinate().z);

        rinexFileHeader.getAntennaMarkerArp().setLeft(antennaLogItem.getMarkerArpUpEcc());
        rinexFileHeader.getAntennaMarkerArp().setMiddle(antennaLogItem.getMarkerArpEastEcc());
        rinexFileHeader.getAntennaMarkerArp().setRight(antennaLogItem.getMarkerArpNorthEcc());

        StringWriter stringWriter = new StringWriter();
        rinexFileHeader.write(stringWriter);

        return ResponseEntity.ok()
            .contentType(MediaType.TEXT_PLAIN)
            .body(stringWriter.toString());
    }
}
