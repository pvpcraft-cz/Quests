package com.leonardobishop.quests.quests.tasktypes.types;

import com.leonardobishop.quests.api.QuestsAPI;
import com.leonardobishop.quests.player.QPlayer;
import com.leonardobishop.quests.player.questprogressfile.QuestProgress;
import com.leonardobishop.quests.player.questprogressfile.QuestProgressFile;
import com.leonardobishop.quests.player.questprogressfile.TaskProgress;
import com.leonardobishop.quests.quests.Quest;
import com.leonardobishop.quests.quests.Task;
import com.leonardobishop.quests.quests.tasktypes.ConfigValue;
import com.leonardobishop.quests.quests.tasktypes.TaskType;
import com.leonardobishop.quests.quests.tasktypes.TaskTypeManager;
import org.bukkit.event.EventHandler;
import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.api.events.BentoBoxEvent;
import world.bentobox.bentobox.database.objects.Island;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

public final class BentoBoxLevel extends TaskType {

  private List<ConfigValue> creatorConfigValues = new ArrayList<>();
  private Field levelField = null;

  public BentoBoxLevel() {
    super("bentobox_level", "Rodney_Mc_Kay, FesterHead",
        "Reach a certain island level.  Requires BentoBox Level addon.");
    this.creatorConfigValues.add(new ConfigValue("level", true, "Minimum island level needed."));
    this.creatorConfigValues
        .add(new ConfigValue(WORLD_KEY, false, "Optional world where this task is valid."));
  }

  @Override
  public List<ConfigValue> getCreatorConfigValues() {
    return creatorConfigValues;
  }

  public static void register(TaskTypeManager manager) {
    if (BentoBox.getInstance().getAddonsManager().getAddonByName("Level").isPresent()) {
      manager.registerTaskType(new BentoBoxLevel());
    }
  }

  @EventHandler
  public void onBentoBoxIslandLevelCalculated(BentoBoxEvent event) {
    Map<String, Object> keyValues = event.getKeyValues();

    if ("IslandLevelCalculatedEvent".equalsIgnoreCase(event.getEventName())) {
      Island island = (Island) keyValues.get("island");

      for (UUID member : island.getMemberSet()) {
        QPlayer qPlayer = QuestsAPI.getPlayerManager().getPlayer(member, true);
        if (qPlayer == null) {
          continue;
        }

        QuestProgressFile questProgressFile = qPlayer.getQuestProgressFile();

        for (Quest quest : super.getRegisteredQuests()) {
          if (questProgressFile.hasStartedQuest(quest)) {
            QuestProgress questProgress = questProgressFile.getQuestProgress(quest);

            for (Task task : quest.getTasksOfType(super.getType())) {
              TaskProgress taskProgress = questProgress.getTaskProgress(task.getId());

              if (taskProgress.isCompleted()) {
                continue;
              }

              long islandLevelNeeded = (long) (int) task.getConfigValue("level");

              Object results = keyValues.get("results");

              try {
                if (levelField == null) {
                  levelField = results.getClass().getDeclaredField("level");
                  levelField.setAccessible(true);
                }

                AtomicLong level = (AtomicLong) levelField.get(results);
                taskProgress.setProgress(level.get());
              } catch (NoSuchFieldException | IllegalAccessException e) {
                e.printStackTrace();
              }

              if (((long) taskProgress.getProgress()) >= islandLevelNeeded) {
                taskProgress.setCompleted(true);
              }
            }
          }
        }
      }
    }
  }
}
