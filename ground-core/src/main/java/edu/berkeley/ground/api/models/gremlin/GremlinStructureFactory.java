package edu.berkeley.ground.api.models.gremlin;

import edu.berkeley.ground.api.models.Structure;
import edu.berkeley.ground.api.models.StructureFactory;
import edu.berkeley.ground.api.versions.Type;
import edu.berkeley.ground.api.versions.gremlin.GremlinItemFactory;
import edu.berkeley.ground.db.DBClient.GroundDBConnection;
import edu.berkeley.ground.db.DbDataContainer;
import edu.berkeley.ground.db.GremlinClient;
import edu.berkeley.ground.db.GremlinClient.GremlinConnection;
import edu.berkeley.ground.exceptions.GroundException;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class GremlinStructureFactory extends StructureFactory {
    private static final Logger LOGGER = LoggerFactory.getLogger(GremlinStructureFactory.class);
    private GremlinClient dbClient;
    private GremlinItemFactory itemFactory;

    public GremlinStructureFactory(GremlinItemFactory itemFactory, GremlinClient dbClient) {
        this.itemFactory = itemFactory;
        this.dbClient = dbClient;
    }

    public Structure create(String name) throws GroundException {
        GremlinConnection connection = this.dbClient.getConnection();

        try {
            String uniqueId = "Structures." + name;

            List<DbDataContainer> insertions = new ArrayList<>();
            insertions.add(new DbDataContainer("name", Type.STRING, name));
            insertions.add(new DbDataContainer("id", Type.STRING, uniqueId));

            connection.addVertex("Structure", insertions);

            connection.commit();
            LOGGER.info("Created structure " + name + ".");

            return StructureFactory.construct(uniqueId, name);
        } catch (GroundException e) {
            connection.abort();

            throw e;
        }
    }

    public Structure retrieveFromDatabase(String name) throws GroundException {
        GremlinConnection connection = this.dbClient.getConnection();

        try {
            List<DbDataContainer> predicates = new ArrayList<>();
            predicates.add(new DbDataContainer("name", Type.STRING, name));
            predicates.add(new DbDataContainer("label", Type.STRING, "Nodes"));

            Vertex vertex = connection.getVertex(predicates);

            String id = (String) vertex.property("id").value();

            connection.commit();
            LOGGER.info("Retrieved structure " + name + ".");

            return StructureFactory.construct(id, name);
        } catch (GroundException e) {
            connection.abort();

            throw e;
        }
    }

    public void update(GroundDBConnection connection, String itemId, String childId, Optional<String> parent) throws GroundException {
        this.itemFactory.update(connection, "Structures." + itemId, childId, parent);
    }
}
