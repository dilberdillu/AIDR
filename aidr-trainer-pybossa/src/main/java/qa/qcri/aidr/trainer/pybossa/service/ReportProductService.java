package qa.qcri.aidr.trainer.pybossa.service;

import qa.qcri.aidr.trainer.pybossa.format.impl.GeoJsonOutputModel;

import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: jlucas
 * Date: 11/22/13
 * Time: 2:28 PM
 * To change this template use File | Settings | File Templates.
 */
public interface ReportProductService {

    void generateCVSReportForGeoClicker() throws Exception;
}
