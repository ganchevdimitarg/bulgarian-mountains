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
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static com.ganchevdimitarg.bulgarianmountains.utils.Constant.*;

@Component
public class ExtractData implements CommandLineRunner {

    private final RestClient restClient;
    private final HutRepository hunRepository;
    private final List<Hut> huts;

    public ExtractData(RestClient restClient, HutRepository hunRepository) {
        this.restClient = restClient;
        this.hunRepository = hunRepository;
        this.huts = new ArrayList<>();
    }

    @Override
    public void run(String... args) {
//        crowingBtsbg();

        try {
            foreachThroughResourceFiles();
            this.hunRepository.saveAll(this.huts);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void crowingBtsbg() {
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
                    createAndWriteToFile(hutUrl, token);
                }
                nextElement = nextElement.nextElementSibling();
            }
        });
    }

    private void foreachThroughResourceFiles() throws IOException {
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        Resource[] resources = resolver.getResources("classpath:btsbg/hizhi/*");


        for (Resource resource : resources) {
            if (resource.isReadable()) {
                try (InputStream inputStream = resource.getInputStream()) {
                    String content = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
                    Document document = Jsoup.parse(content);
                    String url = document.select("meta[property=\"og:url\"]").attr("content");
                    String name = document.select("meta[property=\"og:title\"]").attr("content");
                    mergeChildren(Objects.requireNonNull(document.select("div[class=\"field-item even\"][property=\"content:encoded\"]").first())
                            .children())
                            .forEach(child -> {
//                                if (child.tagName().equals("p")) {
                                String contentText = child.text();
                                this.huts.add(modifyData(contentText, name, url));
//                                }
                            });
                }
            }
        }
    }


    private List<Element> mergeChildren(List<Element> children) {
        StringBuilder mergedContent = new StringBuilder();
        for (Element child : children) {
            String childText = child.removeClass("rtejustify").text().replaceAll("<p class=\"rtejustify\">|<|<p>|</p>|<span data-mce-mark=\"1\">|</span>|<strong>|</strong>", "");
            if (!childText.isEmpty()) {
                mergedContent.append(childText);
            }
        }
        Element element = new Element("p");
        element.text(mergedContent.toString());
        return Collections.singletonList(element);
    }


    private Hut modifyData(String content, String name, String url) {
        List<String> descriptionChunk = getDescriptionChunk(getDescription(content));

        Map<String, String> dataMap = new HashMap<>();
        for (String d : descriptionChunk) {
            String[] chunk = d.split(SPLIT_DELIMITER.getConstant());
            if (chunk.length > 1) {
                dataMap.put(chunk[0].trim(), chunk[1].trim());
            } else {
                dataMap.put(chunk[0].trim(), "");
            }
        }
        Location locationDescription = Location.builder()
                .altitude(dataMap.getOrDefault("надморска височина", "").replace(",|\\.", ""))
                .coordinates(dataMap.getOrDefault("GPS", "").replace(",|\\.", ""))
                .description(dataMap.getOrDefault("Местоположение", "").replace(",|\\.", ""))
                .build();

        Contact contact = Contact.builder()
                .phone(Arrays.stream(dataMap.getOrDefault("За контакти", "").split(";"))
                        .filter(p -> !p.contains("www")).toList())
                .email(dataMap.getOrDefault("еmail", ""))
                .url(url)
                .website(Arrays.stream(dataMap.getOrDefault("За контакти", "").split(","))
                        .filter(p -> p.contains("www")).findFirst().orElse(""))
                .build();
        boolean notWorking = !content.toLowerCase().contains("не работи");
        boolean underconstruction = !content.toLowerCase().contains("в ремонт");
        return Hut.builder()
                .name(name)
                .host(dataMap.getOrDefault("Стопанин", ""))
                .description(dataMap.getOrDefault("Описание", ""))
                .atticFloor(dataMap.getOrDefault("Тавански етаж", ""))
                .mountainRoute(dataMap.getOrDefault("Заслонът е пункт по маршрута", ""))
                .contact(contact)
                .location(locationDescription)
                .startingPoints(Arrays.stream(dataMap.getOrDefault("Изходни пунктове", "").split(";")).toList())
                .neighboringSites(Arrays.stream(dataMap.getOrDefault("Съседни обекти", "").split(",")).map(String::trim).toList())
                .isActive(notWorking)
                .isUnderRepair(underconstruction)
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
        String[] keys = {
                "Местоположение",
                "надморска височина",
                "GPS",
                "Описание",
                "Съседни обекти",
                "Изходен пункт",
                "Изходни пунктове",
                "Стопанин",
                "За контакти",
                "За контакти с хижата",
                "еmail",
                "Тавански етаж",
                "Заслонът е пункт по маршрута",
                "url",
                "тел"
        };

        Map<String, Integer> map = new HashMap<>();
        for (String key : keys) {
            map.put(key, description.indexOf(key + ":"));
        }

        List<Map.Entry<String, Integer>> entryList = new ArrayList<>(map.entrySet());
        entryList.sort(Map.Entry.comparingByValue());

        List<Map.Entry<String, Integer>> filteredEntries = entryList.stream()
                .filter(e -> e.getValue() >= 0)
                .toList();

        return IntStream.range(0, filteredEntries.size())
                .mapToObj(i -> {
                    if (i != filteredEntries.size() - 1) {
                        return description.substring(filteredEntries.get(i).getValue(), filteredEntries.get(i + 1).getValue());
                    } else {
                        return description.substring(filteredEntries.get(i).getValue());
                    }
                })
                .collect(Collectors.toList());
    }

    private String getBody(String url) {
        return this.restClient.get()
                .uri(url)
                .retrieve()
                .body(String.class);
    }


    public void createAndWriteToFile(String fileName, String content) {
        Path filePath = Paths.get("src", "main", "resources", "btsbg", fileName);

        try {
            Files.createDirectories(filePath.getParent());

            Document document = Jsoup.parse(content);

            Files.writeString(filePath, document.html(),
                    StandardOpenOption.CREATE,
                    StandardOpenOption.WRITE,
                    StandardOpenOption.TRUNCATE_EXISTING);

        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }
}