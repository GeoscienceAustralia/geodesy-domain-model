package au.gov.ga.geodesy.domain.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.persistence.Version;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;

import au.gov.ga.geodesy.interfaces.geodesyml.GeodesyMLMarshaller;

@Entity
@Table(name = "CORS_SITE_LOG")
@Configurable(preConstruction = true)
public class CorsSiteLog {
    private static final Log log = LogFactory.getLog(CorsSiteLog.class);

    @Autowired
    @Transient
    private GeodesyMLMarshaller geodesyMLMarshaller;

    @Version
    private Integer version;

    /**
     * RDBMS surrogate key
     */
    @Id
    @Column(name = "ID")
    @GeneratedValue(generator = "surrogateKeyGenerator")
    @SequenceGenerator(name = "surrogateKeyGenerator", sequenceName = "seq_surrogate_keys")
    protected Integer id;

    @Column(name = "FOUR_CHAR_ID", unique = true, nullable = false)
    private String fourCharacterId;

    @SuppressWarnings("unused") // used by hibernate
    private CorsSiteLog() {
    }

    public CorsSiteLog(String id) {
        setFourCharacterId(id);
    }

    public String getFourCharacterId() {
        return fourCharacterId;
    }

    private void setFourCharacterId(String fourCharacterId) {
        this.fourCharacterId = fourCharacterId;
    }
}
