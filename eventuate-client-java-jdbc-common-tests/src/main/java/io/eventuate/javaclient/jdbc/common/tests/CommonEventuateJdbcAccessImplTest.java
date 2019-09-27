package io.eventuate.javaclient.jdbc.common.tests;

import io.eventuate.EntityIdAndType;
import io.eventuate.javaclient.commonimpl.AggregateCrudUpdateOptions;
import io.eventuate.javaclient.commonimpl.EventTypeAndData;
import io.eventuate.javaclient.commonimpl.LoadedEvents;
import io.eventuate.javaclient.commonimpl.SerializedSnapshot;
import io.eventuate.javaclient.jdbc.EventuateJdbcAccess;
import io.eventuate.javaclient.jdbc.SaveUpdateResult;
import org.junit.Assert;
import org.springframework.jdbc.core.JdbcTemplate;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;
import java.util.stream.Collectors;

public abstract class CommonEventuateJdbcAccessImplTest {

  private static final String testAggregate = "testAggregate1";
  private static final String testEventType = "testEventType1";
  private static final String testEventData = "testEventData1";

  protected abstract JdbcTemplate getJdbcTemplate();
  protected abstract EventuateJdbcAccess getEventuateJdbcAccess();

  protected abstract String readAllEventsSql();
  protected abstract String readAllEntitiesSql();
  protected abstract String readAllSnapshots();

  public void testSave() {
    EventTypeAndData eventTypeAndData = new EventTypeAndData(testEventType, testEventData, Optional.empty());

    getEventuateJdbcAccess().save(testAggregate, Collections.singletonList(eventTypeAndData), Optional.empty());

    List<Map<String, Object>> events = getJdbcTemplate().queryForList(readAllEventsSql());
    Assert.assertEquals(1, events.size());

    List<Map<String, Object>> entities = getJdbcTemplate().queryForList(readAllEntitiesSql());
    Assert.assertEquals(1, entities.size());
  }

  public void testFind() {
    EventTypeAndData eventTypeAndData = new EventTypeAndData(testEventType, testEventData, Optional.empty());

    SaveUpdateResult saveUpdateResult = getEventuateJdbcAccess().save(testAggregate, Collections.singletonList(eventTypeAndData), Optional.empty());

    LoadedEvents loadedEvents = getEventuateJdbcAccess().find(testAggregate, saveUpdateResult.getEntityIdVersionAndEventIds().getEntityId(), Optional.empty());

    Assert.assertEquals(1, loadedEvents.getEvents().size());
  }

  public void testUpdate() {
    EventTypeAndData eventTypeAndData = new EventTypeAndData(testEventType, testEventData, Optional.empty());
    SaveUpdateResult saveUpdateResult = getEventuateJdbcAccess().save(testAggregate, Collections.singletonList(eventTypeAndData), Optional.empty());


    EntityIdAndType entityIdAndType = new EntityIdAndType(saveUpdateResult.getEntityIdVersionAndEventIds().getEntityId(), testAggregate);
    eventTypeAndData = new EventTypeAndData("testEventType2", "testEventData2", Optional.empty());

    getEventuateJdbcAccess().update(entityIdAndType,
            saveUpdateResult.getEntityIdVersionAndEventIds().getEntityVersion(),
            Collections.singletonList(eventTypeAndData), Optional.of(new AggregateCrudUpdateOptions(Optional.empty(), Optional.of(new SerializedSnapshot("", "")))));

    List<Map<String, Object>> events = getJdbcTemplate().queryForList(readAllEventsSql());
    Assert.assertEquals(2, events.size());

    List<Map<String, Object>> entities = getJdbcTemplate().queryForList(readAllEntitiesSql());
    Assert.assertEquals(1, entities.size());

    List<Map<String, Object>> snapshots = getJdbcTemplate().queryForList(readAllSnapshots());
    Assert.assertEquals(1, snapshots.size());

    LoadedEvents loadedEvents = getEventuateJdbcAccess().find(testAggregate, saveUpdateResult.getEntityIdVersionAndEventIds().getEntityId(), Optional.empty());
    Assert.assertTrue(loadedEvents.getSnapshot().isPresent());
  }

  protected List<String> loadSqlScriptAsListOfLines(String script) throws IOException {
    try(BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(getClass().getResourceAsStream("/eventuate-embedded-schema.sql")))) {
      return bufferedReader.lines().collect(Collectors.toList());
    }
  }

  protected void executeSql(List<String> sqlList) {
    getJdbcTemplate().execute(sqlList.stream().collect(Collectors.joining("\n")));
  }
}