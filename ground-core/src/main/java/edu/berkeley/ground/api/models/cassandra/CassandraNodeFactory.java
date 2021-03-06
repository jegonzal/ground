package edu.berkeley.ground.api.models.cassandra;

import edu.berkeley.ground.api.models.Node;
import edu.berkeley.ground.api.models.NodeFactory;
import edu.berkeley.ground.api.versions.Type;
import edu.berkeley.ground.api.versions.cassandra.CassandraItemFactory;
import edu.berkeley.ground.db.CassandraClient;
import edu.berkeley.ground.db.CassandraClient.CassandraConnection;
import edu.berkeley.ground.db.DBClient;
import edu.berkeley.ground.db.DBClient.GroundDBConnection;
import edu.berkeley.ground.db.DbDataContainer;
import edu.berkeley.ground.db.QueryResults;
import edu.berkeley.ground.exceptions.GroundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class CassandraNodeFactory extends NodeFactory {
    private static final Logger LOGGER = LoggerFactory.getLogger(CassandraNodeFactory.class);
    private CassandraClient dbClient;

    private CassandraItemFactory itemFactory;

    public CassandraNodeFactory(CassandraItemFactory itemFactory, CassandraClient dbClient) {
        this.dbClient = dbClient;
        this.itemFactory = itemFactory;
    }

    public Node create(String name) throws GroundException {
        CassandraConnection connection = this.dbClient.getConnection();

        try {
            String uniqueId = "Nodes." + name;

            this.itemFactory.insertIntoDatabase(connection, uniqueId);

            List<DbDataContainer> insertions = new ArrayList<>();
            insertions.add(new DbDataContainer("name", Type.STRING, name));
            insertions.add(new DbDataContainer("item_id", Type.STRING, uniqueId));

            connection.insert("Nodes", insertions);

            connection.commit();
            LOGGER.info("Created node " + name + ".");

            return NodeFactory.construct(uniqueId, name);
        } catch (GroundException e) {
            connection.abort();

            throw e;
        }
    }

    public Node retrieveFromDatabase(String name) throws GroundException {
        CassandraConnection connection = this.dbClient.getConnection();

        try {
            List<DbDataContainer> predicates = new ArrayList<>();
            predicates.add(new DbDataContainer("name", Type.STRING, name));

            QueryResults resultSet = connection.equalitySelect("Nodes", DBClient.SELECT_STAR, predicates);
            String id = resultSet.getString(0);

            connection.commit();
            LOGGER.info("Retrieved node " + name + ".");

            return NodeFactory.construct(id, name);
        } catch (GroundException e) {
            connection.abort();

            throw e;
        }
    }

    public void update(GroundDBConnection connection, String itemId, String childId, Optional<String> parent) throws GroundException {
        this.itemFactory.update(connection, itemId, childId, parent);
    }
}
