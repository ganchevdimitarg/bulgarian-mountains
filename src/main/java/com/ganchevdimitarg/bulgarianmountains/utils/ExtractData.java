package com.ganchevdimitarg.bulgarianmountains.utils;

import com.ganchevdimitarg.bulgarianmountains.entites.Hut;
import com.ganchevdimitarg.bulgarianmountains.entites.Location;
import com.ganchevdimitarg.bulgarianmountains.repositories.HutRepository;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class ExtractData implements CommandLineRunner {

    public static final String REGEX = "Местоположение: |GPS: |Описание: |Съседни обекти: |Изходен пункт: |Изходни пунктове: |Стопанин: |За контакти: |За контакти с хижата: |е-mail: |Тавански етаж: |Заслонът е пункт по маршрута: |url: |тел\\. :";
    public static final String DOMAIN = "https://www.btsbg.org";
    public static final String FILE = "tmp.txt";
    public static final String FOR_CONTACT = "За контакти:";

    private final RestClient restClient;
    private final HutRepository hunRepository;
    private final Map<String, Map<String, String>> places;
    private final List<Hut> huts;

    public ExtractData(RestClient restClient, HutRepository hunRepository) {
        this.restClient = restClient;
        this.hunRepository = hunRepository;
        this.places = new TreeMap<>();
        this.huts = new ArrayList<>();
    }

    @Override
    public void run(String... args) {
        List<String> file = readFile(FILE);

        if (file.isEmpty()) {
            crowingBtsbg();
        }

        prepData(file);
        modifyData();
    }

    private void crowingBtsbg() {
        String body = getBody(DOMAIN + "/hizhi");

        Document doc = Jsoup.parse(Objects.requireNonNull(body));
        Elements cities = doc.select("h3");
        cities.forEach(city -> {
            Element nextElement = city.nextElementSibling();

            while (nextElement != null && !nextElement.tagName().equals("h3")) {
                Elements hutLinks = nextElement.select("a[href*='/hizhi/']");

                for (Element link : hutLinks) {
                    String hutUrl = link.attr("href");
                    String token = getBody(DOMAIN + hutUrl);
                    Document document = Jsoup.parse(token);
                    Objects.requireNonNull(document.select("div[class=\"field-item even\"][property=\"content:encoded\"]").first())
                            .children()
                            .forEach(child -> {
                                if (child.tagName().equals("p") /*&& child.hasClass("rtejustify")*/) {
                                    String content = child.text();
                                    StringBuilder builder = new StringBuilder();
                                    writeToFile(
                                            builder.append(city.text())
                                                    .append("| ")
                                                    .append(link.text())
                                                    .append("| ")
                                                    .append(content)
                                                    .append(" url: ")
                                                    .append(DOMAIN)
                                                    .append(hutUrl)
                                                    .append("\n")
                                                    .toString()
                                    );
                                }
                            });
                }
                nextElement = nextElement.nextElementSibling();
            }
        });
    }

    private void prepData(List<String> file) {
        Map<String, String> descriptionMap = new TreeMap<>();
        file.forEach(d -> {
            String[] token = d.split("\\| ");
            String city = token[0];
            String name = token[1];
            String description = token[2];
            descriptionMap.merge(name, description, (o, n) -> o + " " + n);
            this.places.putIfAbsent(city, new TreeMap<>());
            this.places.get(city).put(name, descriptionMap.get(name));
        });
    }

    private void modifyData() {

        this.places.forEach((city, content) -> {

            for (Map.Entry<String, String> entry : content.entrySet()) {
                String name = entry.getKey();
                String description = entry.getValue();
                if (!description.contains("Местоположение: ")) {
                    description = "Местоположение: " + description;
                }
                if (!description.contains("Описание:")) {
                    description = description.replace("ºЕ.", "ºЕ. Описание:");
                }

                description = description.replace("Изходен пункт", "Изходни пунктове")
                        .replace("За контакти с обекта:", FOR_CONTACT)
                        .replace("За контакти с хижара - Николай Шмикеров:", FOR_CONTACT)
                        .replace("За контакти с хижата:", FOR_CONTACT)
                        .replace("За контакти :", FOR_CONTACT)
                        .replace("За контакти на хижата :", FOR_CONTACT)
                        .replace("За контакти с обекта:", FOR_CONTACT)
                        .replace("за контакти. ", FOR_CONTACT)
                        .replace("За контакти и резервации:", FOR_CONTACT)
                        .replace("Контакти за обекта :", FOR_CONTACT)
                        .replace("За контакти с туристическото дружество:", FOR_CONTACT)
                        .replace("e-mail:", "email: ");

                List<String> descriptionChunk = getDescriptionChunk(description);
                String location = "";
                String altitue = "";
                String gps = "";
                String des = "";
                String nearbyObjects = "";
                String startingPoint = "";
                String host = "";
                String contact = "";
                String email = "";
                String atticFloor = "";
                String shelter = "";
                String url = "";
                String phone = "";
                for (String d : descriptionChunk) {
                    if (d.contains("Местоположение")) {
                        location = d.split(":")[1].trim();
                        String regex = "(\\d+)\\s*м\\s*н\\.?\\s*в\\.?";

                        Matcher matcher = Pattern.compile(regex).matcher(location);
                        if (matcher.find()) {
                            altitue = matcher.group(1);
                        } else {
                            System.out.println("No match");
                        }

                    } else if (d.contains("GPS")) {
                        gps = d.split(":")[1].trim();
                    } else if (d.contains("Описание")) {
                        des = d.split(":")[1].trim();
                    } else if (d.contains("Съседни обекти")) {
                        nearbyObjects = d.split(":")[1].trim();
                    } else if (d.contains("Изходен пункт")) {
                        startingPoint = d.split(":")[1].trim();
                    } else if (d.contains("Изходни пунктове")) {
                        startingPoint = d.split(":")[1].trim();
                    } else if (d.contains("Стопанин")) {
                        host = d.split(":")[1].trim();
                    } else if (d.contains("За контакти")) {
                        contact = d.split(":")[1].trim();
                    } else if (d.contains("За контакти с хижата")) {
                        contact = d.split(":")[1].trim();
                    } else if (d.contains("е-mail")) {
                        email = d.split(":")[1].trim();
                    } else if (d.contains("Тавански етаж")) {
                        atticFloor = d.split(":")[1].trim();
                    } else if (d.contains("Заслонът е пункт по маршрута")) {
                        shelter = d.split(":")[1].trim();
                    } else if (d.contains("url")) {
                        url = d.split(":")[1].trim();
                    } else if (d.contains("тел")) {
                        phone = d.split(":")[1].trim();
                    }
                }

                Location hutLocation = Location.builder()
                        .altitue(altitue)
                        .coordinates(gps)
                        .description(location)
                        .build();
                Hut hut = Hut.builder()
                        .city(city)
                        .name(name)
                        .uri(DOMAIN + "/hizhi/" + name)
                        .location(hutLocation)
                        .description(des)
                        .neighboringSites(nearbyObjects)
                        .isActive(true)
                        .isUnderRepair(false)
                        .build();

                System.out.println();

            }
        });
    }

    private List<String> getDescriptionChunk(String description) {
        Map<String, Integer> map = new HashMap<>();
        map.put("Местоположение", description.indexOf("Местоположение:"));
        map.put("GPS", description.indexOf("GPS:"));
        map.put("Описание", description.indexOf("Описание:"));
        map.put("Съседни обекти", description.indexOf("Съседни обекти:"));
        map.put("Изходен пункт", description.indexOf("Изходен пункт:"));
        map.put("Изходни пунктове", description.indexOf("Изходни пунктове:"));
        map.put("Стопанин", description.indexOf("Стопанин:"));
        map.put("За контакти", description.indexOf(FOR_CONTACT));
        map.put("За контакти с хижата", description.indexOf("За контакти с хижата:"));
        map.put("еmail", description.indexOf("email:"));
        map.put("Тавански етаж", description.indexOf("Тавански етаж:"));
        map.put("Заслонът е пункт по маршрута", description.indexOf("Заслонът е пункт по маршрута:"));
        map.put("url", description.indexOf("url:"));
        map.put("тел", description.indexOf("тел. :"));

        List<Map.Entry<String, Integer>> entryList = new ArrayList<>(map.entrySet());
        entryList.sort(Map.Entry.comparingByValue());
        List<Map.Entry<String, Integer>> entryStream = entryList.stream()
                .filter(e -> e.getValue() >= 0)
                .toList();

        List<String> descriptionChunk = new ArrayList<>();
        for (int i = 0; i < entryStream.size(); i++) {
            if (i != entryStream.size() - 1) {
                descriptionChunk.add(description.substring(entryStream.get(i).getValue(), entryStream.get(i + 1).getValue()));
            } else {
                descriptionChunk.add(description.substring(entryStream.get(i).getValue()));
            }
        }
        return descriptionChunk;
    }

    private List<String> readFile(String filePath) {
        try {
            return Files.readAllLines(Path.of(filePath));
        } catch (IOException e) {
            return List.of();
        }
    }

    private void writeToFile(String builder) {
        try (FileWriter writer = new FileWriter(FILE, true)) {
            writer.write(builder);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String getBody(String url) {
        return this.restClient.get()
                .uri(url)
                .retrieve()
                .body(String.class);
    }

    private long toMilliseconds(String timeStr) {
        if (timeStr.contains(".")) {
            String[] parts = timeStr.split("\\.");
            int hours = Integer.parseInt(parts[0]);
            int minutes = Integer.parseInt(parts[1]);
            return (hours * 3600000L) + (minutes * 60000L);
        } else {
            int hours = Integer.parseInt(timeStr);
            return hours * 3600000L;
        }
    }
}
