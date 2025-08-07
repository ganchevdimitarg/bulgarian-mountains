package com.ganchevdimitarg.bulgarianmountains.utils;

import com.ganchevdimitarg.bulgarianmountains.entites.Contact;
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

import java.util.*;

import static com.ganchevdimitarg.bulgarianmountains.utils.Constant.*;

@Component
public class ExtractData implements CommandLineRunner {

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
        crowingBtsbg();
    }

    private void crowingBtsbg() {
        List<Hut> huts = new ArrayList<>();
        String body = getBody(DOMAIN.getConstant() + "/hizhi");

        Document doc = Jsoup.parse(Objects.requireNonNull(body));
        Elements cities = doc.select("h3");
        cities.forEach(city -> {
            Element nextElement = city.nextElementSibling();

            while (nextElement != null && !nextElement.tagName().equals("h3")) {
                Elements hutLinks = nextElement.select("a[href*='/hizhi/']");

                for (Element link : hutLinks) {
                    String hutUrl = link.attr("href");
                    String token = getBody(DOMAIN.getConstant() + hutUrl);
                    Document document = Jsoup.parse(token);
                    Objects.requireNonNull(document.select("div[class=\"field-item even\"][property=\"content:encoded\"]").first())
                            .children()
                            .forEach(child -> {
                                if (child.tagName().equals("p")) {
                                    String content = child.text();
                                    String url = DOMAIN.getConstant() + hutUrl;
                                    huts.add(modifyData(content, city.text(), link.text(), url));
                                }
                            });
                }
                nextElement = nextElement.nextElementSibling();
            }
        });
        huts.forEach(System.out::println);
    }

    private Hut modifyData(String content, String city, String name, String url) {
        List<String> descriptionChunk = getDescriptionChunk(getDescription(content));

        String location = "";
        String altitude = "";
        String gps = "";
        String des = "";
        List<String> nearbyObjects = new ArrayList<>();
        List<String> startingPoints = new ArrayList<>();
        String host = "";
        String email = "";
        String atticFloor = "";
        String mountainRoute = "";
        List<String> phone = new ArrayList<>();
        for (String d : descriptionChunk) {
            String[] chunk = d.split(SPLIT_DELIMITER.getConstant());
            if (d.contains("Местоположение")) {
                location = chunk.length > 1 ? chunk[1].trim() : chunk[0].trim();
            } else if (d.contains("надморска височина")) {
                altitude = chunk.length > 1 ? chunk[1].trim() : chunk[0].trim();
            } else if (d.contains("GPS")) {
                gps = chunk.length > 1 ? chunk[1].trim() : chunk[0].trim();
            } else if (d.contains("Описание")) {
                des = chunk.length > 1 ? chunk[1].trim() : chunk[0].trim();
            } else if (d.contains("Съседни обекти")) {
                String chunkTmp;
                if (chunk.length > 1) {
                    chunkTmp = chunk[1];
                } else {
                    chunkTmp = chunk[0];
                }
                if (chunkTmp.contains(";")) {
                    nearbyObjects = splitChunck(chunkTmp, ";");
                } else {
                    nearbyObjects = splitChunck(chunkTmp, ",");
                }
            } else if (d.contains("Изходен пункт")) {
                startingPoints.addAll(Arrays.stream(chunk.length > 1 ? chunk[1].trim().split(";") : chunk[0].trim().split(";")).toList());
            } else if (d.contains("Изходни пунктове")) {
                startingPoints.addAll(Arrays.stream(chunk.length > 1 ? chunk[1].trim().split(";") : chunk[0].trim().split(";")).toList());
            } else if (d.contains("Стопанин")) {
                host = chunk.length > 1 ? chunk[1].trim() : chunk[0].trim();
            } else if (d.contains("е-mail")) {
                email = chunk.length > 1 ? chunk[1].trim() : chunk[0].trim();
            } else if (d.contains("Тавански етаж")) {
                atticFloor = chunk.length > 1 ? chunk[1].trim() : chunk[0].trim();
            } else if (d.contains("Заслонът е пункт по маршрута")) {
                mountainRoute = chunk.length > 1 ? chunk[1].trim() : chunk[0].trim();
            } else if (d.contains("За контакти")) {
                phone.add(chunk.length > 1 ? chunk[1].trim() : chunk[0].trim());
            } else if (d.contains("За контакти с хижата")) {
                phone.add(chunk.length > 1 ? chunk[1].trim() : chunk[0].trim());
            } else if (d.contains("тел")) {
                phone.addAll(Arrays.stream(chunk.length > 1 ? chunk[1].trim().split(";") : chunk[0].trim().split("; ")).toList());
            } else if (d.contains("на телефони")) {
                phone.addAll(Arrays.stream(chunk.length > 1 ? chunk[1].trim().split(";") : chunk[0].trim().split("; ")).toList());
            } else if (d.contains("на телефон")) {
                phone.addAll(Arrays.stream(chunk.length > 1 ? chunk[1].trim().split(";") : chunk[0].trim().split("; ")).toList());
            }
        }

        nearbyObjects = nearbyObjects.stream().map(String::trim).toList();

        Location locationDescription = Location.builder()
                .altitude(altitude)
                .coordinates(gps)
                .description(location)
                .build();

        Contact contact = Contact.builder()
                .phone(phone)
                .email(email)
                .url(url)
                .build();

        return Hut.builder()
                .city(city)
                .name(name)
                .host(host)
                .description(des)
                .atticFloor(atticFloor)
                .mountainRoute(mountainRoute)
                .contact(contact)
                .location(locationDescription)
                .startingPoints(startingPoints)
                .neighboringSites(nearbyObjects)
                .isActive(!content.toLowerCase().contains("не работи"))
                .isUnderRepair(!content.toLowerCase().contains("в ремонт"))
                .build();

    }

    private static List<String> splitChunck(String chunck, String delimiter) {
        return Arrays.stream(chunck.trim().split(delimiter)).toList();
    }

    private String getDescription(String description) {
        if (!description.contains("Местоположение: ")) {
            description = "Местоположение: " + description;
        }
        if (!description.contains("Описание:")) {
            description = description.replace("ºЕ.", "ºЕ. Описание:");
        }

        return description.replace("Изходен пункт", "Изходни пунктове")
                .replace(FIND_CONTACT_VARIANCE_REGEX.getConstant(), FOR_CONTACT.getConstant())
                .replaceAll(ALTITUDE_REGEX.getConstant(), "надморска височина: $1");

    }

    private List<String> getDescriptionChunk(String description) {
        Map<String, Integer> map = new HashMap<>();
        map.put("Местоположение", description.indexOf("Местоположение:"));
        map.put("надморска височина", description.indexOf("надморска височина:"));
        map.put("GPS", description.indexOf("GPS:"));
        map.put("Описание", description.indexOf("Описание:"));
        map.put("Съседни обекти", description.indexOf("Съседни обекти:"));
        map.put("Изходен пункт", description.indexOf("Изходен пункт:"));
        map.put("Изходни пунктове", description.indexOf("Изходни пунктове:"));
        map.put("Стопанин", description.indexOf("Стопанин:"));
        map.put("За контакти", description.indexOf(FOR_CONTACT.getConstant()));
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

    private String getBody(String url) {
        return this.restClient.get()
                .uri(url)
                .retrieve()
                .body(String.class);
    }
}