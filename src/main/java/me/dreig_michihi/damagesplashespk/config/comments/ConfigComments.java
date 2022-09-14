package me.dreig_michihi.damagesplashespk.config.comments;

import com.projectkorra.projectkorra.ProjectKorra;
import me.dreig_michihi.damagesplashespk.DamageSplashesPK;
import me.dreig_michihi.damagesplashespk.config.SplashesConfig;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.ListIterator;

public class ConfigComments {

    public static void addDefaultComment(String configPath, String comment, boolean inline) {
        try {
            Path path = SplashesConfig.getFile().toPath();
            List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);
            if (!inline)
                lines.add(0, "");
            ListIterator<String> configPathSeparated = List.of(configPath.split("\\.")).listIterator();
            ListIterator<String> iter = lines.listIterator();
            String stringFinder = configPathSeparated.next();
            while (iter.hasNext()) {
                String curr = iter.next();
                if (curr.trim().startsWith(stringFinder)) {
                    if (configPathSeparated.hasNext())
                        stringFinder = configPathSeparated.next();
                    else {
                        if (inline) {
                            if (!curr.contains("#")) {
                                iter.set(curr + " # " + comment);
                            }
                        } else {
                            iter.previous();
                            String prev = iter.previous();
                            iter.next();
                            if (!prev.trim().startsWith("#")) {
                                iter.add("# " + comment);
                            }
                            iter.next();
                        }
                        break;
                    }
                }
            }
            if (!inline)
                lines.remove(0);
            Files.write(path, lines, StandardCharsets.UTF_8);
        } catch (IOException e) {
            DamageSplashesPK.plugin.getLogger().info("COMMENTS ERROR");
            //throw new RuntimeException(e);
        }
    }
}
