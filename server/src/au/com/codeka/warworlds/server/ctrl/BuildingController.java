package au.com.codeka.warworlds.server.ctrl;

import java.sql.Statement;

import org.joda.time.DateTime;

import au.com.codeka.common.model.BaseBuilding;
import au.com.codeka.common.model.BuildingDesign;
import au.com.codeka.common.model.DesignKind;
import au.com.codeka.common.protobuf.Messages;
import au.com.codeka.warworlds.server.RequestException;
import au.com.codeka.warworlds.server.data.SqlStmt;
import au.com.codeka.warworlds.server.data.Transaction;
import au.com.codeka.warworlds.server.model.Building;
import au.com.codeka.warworlds.server.model.Colony;
import au.com.codeka.warworlds.server.model.DesignManager;
import au.com.codeka.warworlds.server.model.Star;

public class BuildingController {
    private DataBase db;

    public BuildingController() {
        db = new DataBase();
    }
    public BuildingController(Transaction trans) {
        db = new DataBase(trans);
    }

    public Building createBuilding(Star star, Colony colony, String designID) throws RequestException {
        try {
            Building building = new Building(star, colony, designID);
            db.createBuilding(colony, building);
            colony.getBuildings().add(building);
            return building;
        } catch(Exception e) {
            throw new RequestException(e);
        }
    }

    public Building upgradeBuilding(Star star, Colony colony, int buildingID) throws RequestException {
        Building existingBuilding = null;
        for (BaseBuilding building : colony.getBuildings()) {
            if (((Building) building).getID() == buildingID) {
                existingBuilding = (Building) building;
                break;
            }
        }
        if (existingBuilding == null) {
            throw new RequestException(404);
        }

        BuildingDesign design = (BuildingDesign) DesignManager.i.getDesign(DesignKind.BUILDING, existingBuilding.getDesignID());
        if (existingBuilding.getLevel() >= design.getUpgrades().size()) {
            return existingBuilding;
        }

        try {
            db.upgradeBuilding(existingBuilding);
            return existingBuilding;
        } catch(Exception e) {
            throw new RequestException(e);
        }
    }

    private static class DataBase extends BaseDataBase {
        public DataBase() {
            super();
        }
        public DataBase(Transaction trans) {
            super(trans);
        }

        public void createBuilding(Colony colony, Building building) throws Exception {
            String sql = "INSERT INTO buildings (star_id, colony_id, empire_id," +
                                               " design_id, build_time, level)" +
                        " VALUES (?, ?, ?, ?, ?, ?)";
            SqlStmt stmt = prepare(sql, Statement.RETURN_GENERATED_KEYS);
            stmt.setInt(1, colony.getStarID());
            stmt.setInt(2, colony.getID());
            if (colony.getEmpireKey() == null) {
                stmt.setNull(3);
            } else {
                stmt.setInt(3, colony.getEmpireID());
            }
            stmt.setString(4, building.getDesignID());
            stmt.setDateTime(5, DateTime.now());
            stmt.setInt(6, 1);
            stmt.update();
            building.setID(stmt.getAutoGeneratedID());
        }

        public void upgradeBuilding(Building building) throws Exception {
            String sql = "UPDATE buildings SET level = level+1 WHERE id = ?";
            SqlStmt stmt = prepare(sql);
            stmt.setInt(1, building.getID());
            stmt.update();
            building.setLevel(building.getLevel()+1);
        }
    }
}
