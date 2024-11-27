package com.raidtracker.filereadwriter;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;
import com.google.inject.Inject;
import com.raidtracker.RaidTracker;
import com.raidtracker.RaidTrackerItem;
import com.raidtracker.RaidType;
import java.util.Arrays;
import java.util.Iterator;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static net.runelite.client.RuneLite.RUNELITE_DIR;


@Slf4j
public class FileReadWriter {

	@Getter
	private String accountHash;
	@Setter
	@Getter
	private String username = "";
	private String coxDir;
	private String tobDir;
	private String toaDir;
    private String defaultDir;

	@Setter
	private boolean accountMigrated;

	@Inject
	private Gson gson;

	public void migrate() {
		File dir = new File(RUNELITE_DIR, "raid-data tracker");

		File dir_user = new File(dir, username);
		File dir_cox = new File(dir, "cox");
		File dir_tob = new File(dir, "tob");
		File dir_toa = new File(dir, "toa");

		String old_file_name = "raid_tracker_data.log";

		File old_user_cox = new File(dir_user + "\\cox\\" + old_file_name);
		File old_user_tob = new File(dir_user + "\\tob\\" + old_file_name);
		File old_user_toa = new File(dir_user + "\\toa\\" + old_file_name);

		if (old_user_cox.isFile()) {
			migrateLog(old_user_cox, RaidType.COX);
		}
		if (old_user_tob.isFile()) {
			migrateLog(old_user_tob, RaidType.TOB);
		}
		if (old_user_toa.isFile()) {
			migrateLog(old_user_toa, RaidType.TOA);
		}

		File old_cox = new File(dir_cox, old_file_name);
		File old_tob = new File(dir_tob, old_file_name);
		File old_toa = new File(dir_toa, old_file_name);

		if (old_cox.isFile()) {
			migrateLog(old_user_cox, RaidType.COX);
		}
		if (old_tob.isFile()) {
			migrateLog(old_user_tob, RaidType.TOB);
		}
		if (old_toa.isFile()) {
			migrateLog(old_user_toa, RaidType.TOA);
		}
	}

	public void writeToFile(RaidTracker raidTracker, RaidType raidType)
	{
		String dir;

		if (raidTracker.isInTheatreOfBlood()) {
			dir = tobDir;
		} else if (raidTracker.isInTombsOfAmascut()) {
			dir = toaDir;
		} else if (raidTracker.isInRaidChambers()) {
			dir = coxDir;
		} else {
			dir = defaultDir;
			log.warn("writeToFile called without an inRaid flag set.", new IllegalStateException());
		}

		try
		{
			log.info("writer started");
			//use json format so serializing and deserializing is easy
			JsonParser parser = new JsonParser();

			String fileName = dir + "\\" + raidType.name().toLowerCase() + ".log";
//			String fileName = dir + "\\raid_tracker_data.log";
			FileWriter fw = new FileWriter(fileName,true); //the true will append the new data
			gson.toJson(parser.parse(getJSONString(raidTracker, gson, parser)), fw);
			fw.append("\n");
			fw.close();
		}
		catch(IOException ioe)
		{
			System.err.println("IOException: " + ioe.getMessage() + " in writeToFile");
		}
	}

	public String getJSONString(RaidTracker raidTracker, Gson gson, JsonParser parser)
	{
		JsonObject RTJson =  parser.parse(gson.toJson(raidTracker)).getAsJsonObject();


		List<RaidTrackerItem> lootList = raidTracker.getLootList();

		//------------------ temporary fix until I can get gson.tojson to work for arraylist<RaidTrackerItem> ---------
		JsonArray lootListToString = new JsonArray();


		for (RaidTrackerItem item : lootList) {
			lootListToString.add(parser.parse(gson.toJson(item, new TypeToken<RaidTrackerItem>() {
			}.getType())));
		}

		RTJson.addProperty("lootList", lootListToString.toString());

		//-------------------------------------------------------------------------------------------------------------

		//massive bodge, works for now
		return RTJson.toString().replace("\\\"", "\"").replace("\"[", "[").replace("]\"", "]");
	}

	public ArrayList<RaidTracker> readFromFile(String alternateFile, RaidType raidType)
	{
		String dir = getRaidDirectory(raidType);

		String fileName;
		if (alternateFile.length() != 0) {
			fileName = alternateFile;
		} else {
			fileName = dir + "\\" + raidType.name().toLowerCase() + ".log";
//			fileName = dir + "\\raid_tracker_data.log";
		}

		try {
			JsonParser parser = new JsonParser();

			BufferedReader bufferedreader = new BufferedReader(new FileReader(fileName));
			String line;

			ArrayList<RaidTracker> RTList = new ArrayList<>();
			while ((line = bufferedreader.readLine()) != null && line.length() > 0) {
				try {
					RaidTracker parsed = gson.fromJson(parser.parse(line), RaidTracker.class);
					RTList.add(parsed);
				} catch (JsonSyntaxException e) {
					System.out.println("Bad line: " + line);
				}

			}

			bufferedreader.close();
			return RTList;
		} catch (IOException e) {
			e.printStackTrace();
			return new ArrayList<>();
		}
	}

	public ArrayList<RaidTracker> readFromFile() {
		return readFromFile("", RaidType.COX);
	}

	public ArrayList<RaidTracker> readFromFile(RaidType raidType) {
		return readFromFile("", raidType);
	}

	public void createFolders()
	{
		File dir = new File(RUNELITE_DIR, "raid-data tracker");
		IGNORE_RESULT(dir.mkdir());
		dir = new File(dir, username);
		IGNORE_RESULT(dir.mkdir());
		File dir_cox = new File(dir, "cox");
		File dir_tob = new File(dir, "tob");
		File dir_toa = new File(dir, "toa");
        File dir_default = new File(dir, "unknown");
		IGNORE_RESULT(dir_cox.mkdir());
		IGNORE_RESULT(dir_tob.mkdir());
		IGNORE_RESULT(dir_toa.mkdir());
        IGNORE_RESULT(dir_default.mkdir());
		File newCoxFile = new File(dir_cox + "\\raid_tracker_data.log");
		File newTobFile = new File(dir_tob + "\\raid_tracker_data.log");
		File newToaFile = new File(dir_toa + "\\raid_tracker_data.log");
        File newDefaultFile = new File(dir_default + "\\raid_tracker_data.log");

		try {
			IGNORE_RESULT(newCoxFile.createNewFile());
			IGNORE_RESULT(newTobFile.createNewFile());
			IGNORE_RESULT(newToaFile.createNewFile());
            IGNORE_RESULT(newDefaultFile.createNewFile());
		} catch (IOException e) {
			e.printStackTrace();
		}

		this.coxDir = dir_cox.getAbsolutePath();
		this.tobDir = dir_tob.getAbsolutePath();
		this.toaDir = dir_toa.getAbsolutePath();
        this.defaultDir = dir_default.getAbsolutePath();
	}

	public void createFoldersHash() {
		File dir = new File(RUNELITE_DIR, "raid-data tracker");
		IGNORE_RESULT(dir.mkdir());

		File[] files = dir.listFiles();
		if(files != null)
		{
			Iterator<File> iterator = Arrays.stream(files).iterator();
			while (iterator.hasNext()) {
				File f = iterator.next();

				if(f.isDirectory() && f.getName().contains(accountHash))
				{
					if (f.getName().contains(username)) {
						log.info("Active log dir set to: " + f.getName());
						dir = new File(dir + "\\" + f.getName());
						break;
					}

					dir = new File(dir, username + "_" + accountHash);
					f.renameTo(dir);
					log.info("Username out of date, folder renamed and active log dir set to: " + dir);
					break;
				}

				if (!iterator.hasNext()) {
					dir = new File(dir, username + "_" + accountHash);
					IGNORE_RESULT(dir.mkdir());
					System.out.println("No migrated folder found for user, active log dir created and set to: " + dir);
				}
			}
		}

		File newCoxFile = new File(dir, "cox.log");
		File newTobFile = new File(dir, "tob.log");
		File newToaFile = new File(dir, "toa.log");

		try {
			IGNORE_RESULT(newCoxFile.createNewFile());
			IGNORE_RESULT(newTobFile.createNewFile());
			IGNORE_RESULT(newToaFile.createNewFile());
		} catch (IOException e) {
			e.printStackTrace();
		}

		this.coxDir = dir.getAbsolutePath();
		this.tobDir = dir.getAbsolutePath();
		this.toaDir = dir.getAbsolutePath();
	}

	public void updateUsername(final String username) {
		this.username = username;
		createFolders();
	}

	public void updateUsernameHash(final String accountHash, final  String username) {
		this.accountHash = accountHash;
		this.username = username;
		createFoldersHash();
	}

	// Used for making sure ToA loot and points is accurate
	// Initial write made on reward chest interface opened,
	// this updates after player leaves to account for purples/pets others receive
	public void updateRTLog(RaidTracker raidTracker, RaidType raidType) {
		String dir = getRaidDirectory(raidType);

		try {
			JsonParser parser = new JsonParser();

			String fileName = dir + "\\" + raidType.name().toLowerCase() + ".log";
//			String fileName = dir + "\\raid_tracker_data.log";

			ArrayList<RaidTracker> RTList = readFromFile(raidType);

			FileWriter fw = new FileWriter(fileName, false); //the true will append the new data

			for (RaidTracker RT : RTList) {

				if (RT.getUniqueID().equals(raidTracker.getUniqueID()) && !RT.equals(raidTracker)) {
					log.info("writer updated log");
					RT = raidTracker;
				}

				gson.toJson(parser.parse(getJSONString(RT, gson, parser)), fw);

				fw.append("\n");
			}

			fw.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void updateRTList(ArrayList<RaidTracker> RTList, RaidType raidType) {
		String dir = getRaidDirectory(raidType);

		try {
			JsonParser parser = new JsonParser();

			String fileName = dir + "\\" + raidType.name().toLowerCase() + ".log";
//			String fileName = dir + "\\raid_tracker_data.log";


			FileWriter fw = new FileWriter(fileName, false); //the true will append the new data

			for (RaidTracker RT : RTList) {
				if (RT.getLootSplitPaid() > 0) {
					RT.setSpecialLootInOwnName(true);
				}
				else {
					//bit of a wonky check, so try to avoid with lootsplitpaid if possible
					RT.setSpecialLootInOwnName(!RT.getLootList().isEmpty() && RT.getLootList().get(0).getName().equalsIgnoreCase(RT.getSpecialLoot()));
				}

				gson.toJson(parser.parse(getJSONString(RT, gson, parser)), fw);

				fw.append("\n");
			}

			fw.close();

		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void migrateLog(File oldLog, RaidType raidType) {
		File dir = new File(RUNELITE_DIR, "raid-data tracker");

		try {
			JsonParser parser = new JsonParser();

			String fileName = dir + "\\" + username + "_" + accountHash + "\\" + raidType.name().toLowerCase() + ".log";

			FileWriter fw = new FileWriter(fileName, false); //the true will append the new data

			ArrayList<RaidTracker> oldRTList = readFromFile(String.valueOf(oldLog), raidType);
			ArrayList<RaidTracker> newRTList = readFromFile(fileName, raidType);

			for (RaidTracker oldRT : oldRTList)
			{

				Iterator<RaidTracker> iterator = newRTList.iterator();
				while (iterator.hasNext()) {
					RaidTracker currentRT = iterator.next();

					if (oldRT.getUniqueID().equals(currentRT.getUniqueID())) {
						break;
					}
				}
				if (!iterator.hasNext()) {
					gson.toJson(parser.parse(getJSONString(oldRT, gson, parser)), fw);
					fw.append("\n");
				}
			}
			fw.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public boolean delete(RaidType raidType) {
		String dir = getRaidDirectory(raidType);

		File newFile = new File(dir, raidType.name().toLowerCase() + ".log");
//		File newFile = new File(dir + "\\raid_tracker_data.log");

		boolean isDeleted = newFile.delete();

		try {
			IGNORE_RESULT(newFile.createNewFile());
		} catch (IOException e) {
			e.printStackTrace();
		}

		return isDeleted;
	}

	public void IGNORE_RESULT(boolean b) {}

	public String getRaidDirectory(RaidType raidType) {
		switch(raidType) {
            case COX:
                return coxDir;
			case TOB:
				return tobDir;
			case TOA:
				return toaDir;
            default:
                return defaultDir;
		}
	}
}
