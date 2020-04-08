package au.gov.ga.geodesy.port.adapter.rest;

import au.gov.ga.geodesy.domain.model.sitelog.SiteLog;
import au.gov.ga.geodesy.domain.service.CorsSiteLogService;
import au.gov.ga.geodesy.port.adapter.geodesyml.GeodesyMLSiteLogReader;
import au.gov.ga.geodesy.support.TestResources;
import au.gov.ga.geodesy.support.spring.IntegrationTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.test.annotation.Rollback;
import org.testng.annotations.Test;

import static io.restassured.module.mockmvc.RestAssuredMockMvc.given;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;

public class SkeletonEndpointITest extends IntegrationTest {

    @Autowired
    private CorsSiteLogService siteLogService;

    @Test
    @Rollback(false)
    public void upload() throws Exception {
        SiteLog alic = new GeodesyMLSiteLogReader(TestResources.customGeodesyMLSiteLogReader("ALIC")).getSiteLog();
        siteLogService.upload(alic);
    }

    @Test(dependsOnMethods = {"upload"})
    @Rollback(false)
    public void testFindSkeletonHeaderForALIC00AUS() throws Exception {
        String text = given()
            .when()
            .get("/skeletonFiles/ALIC00AUS.SKL")
            .then()
            .log().body()
            .statusCode(HttpStatus.OK.value())
            .contentType("text/plain")
            .extract().body().asString();

        assertThat(text, containsString("ANTENNA: DELTA"));
        assertThat(text, containsString("50137M001"));
    }

    @Test(dependsOnMethods = {"upload"})
    @Rollback(false)
    public void testFindSkeletonHeaderForALIC() throws Exception {
        String text = given()
            .when()
            .get("/skeletonFiles/alic.skl")
            .then()
            .log().body()
            .statusCode(HttpStatus.OK.value())
            .contentType("text/plain")
            .extract().body().asString();

        assertThat(text, containsString("AGENCY"));
        assertThat(text, containsString("50137M001"));
    }

    @Test
    public void testNotFound() throws Exception {
        given()
            .when()
            .get("/skeletonFiles/0000.SKL")
            .then()
            .statusCode(HttpStatus.NOT_FOUND.value());
    }

    @Test
    public void testInvalid() throws Exception {
        given()
            .when()
            .get("/skeletonFiles/123456789.xyz")
            .then()
            .statusCode(HttpStatus.NOT_FOUND.value());
    }
}