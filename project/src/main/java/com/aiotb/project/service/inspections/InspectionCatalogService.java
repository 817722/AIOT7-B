package com.aiotb.project.service.inspections;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import com.aiotb.project.dto.inspections.InspectionDto;

import jakarta.annotation.PostConstruct;

// 클라이언트가 .csv내에 존재하지 않는 교량을 임의로 작성하면 안 됨.
// 그렇기 때문에 .csv만 불러올 수 있도록 작업함.
@Service
public class InspectionCatalogService {
    private static final String CSV_PATH = "data/bridges_20251231.csv";
    private final Map<String, InspectionDto.CatalogInspection> inspectionsById = new LinkedHashMap<>();
    private List<InspectionDto.CatalogInspection> allinspections = List.of();

    @PostConstruct
    public void loadCatalog() {
        List<InspectionDto.CatalogInspection> loaded = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                new ClassPathResource(CSV_PATH).getInputStream(), StandardCharsets.UTF_8))) {
            String headerLine = reader.readLine();
            if (headerLine == null)
                throw new IllegalStateException("교량 CSV 파일이 비어 있습니다.");
            List<String> headers = parseCsvLine(headerLine.replace("\uFEFF", ""));
            Map<String, Integer> columns = new HashMap<>();
            for (int i = 0; i < headers.size(); i++)
                columns.put(headers.get(i).trim(), i);

            String[] required = { "교량명", "시설물종별등급구분", "도로종류", "도로노선명", "시도명", "시군구명", "소재지지번주소", "교량연장", "교량폭", "관리기관명",
                    "데이터기준일자" };
            for (String column : required) {
                if (!columns.containsKey(column))
                    throw new IllegalStateException("CSV 필수 컬럼이 없습니다: " + column);
            }

            String line;
            int rowNumber = 0;
            while ((line = reader.readLine()) != null) {
                if (line.isBlank())
                    continue;
                rowNumber++;
                List<String> row = parseCsvLine(line);
                String name = value(row, columns, "교량명");
                if (name.isBlank())
                    continue;
                String catalogId = "CSV-" + String.format(Locale.ROOT, "%06d", rowNumber);
                InspectionDto.CatalogInspection inspection = new InspectionDto.CatalogInspection(
                        catalogId, name,
                        value(row, columns, "시설물종별등급구분"),
                        value(row, columns, "도로종류"),
                        value(row, columns, "도로노선명"),
                        value(row, columns, "시도명"),
                        value(row, columns, "시군구명"),
                        value(row, columns, "소재지지번주소"),
                        decimal(value(row, columns, "교량연장")),
                        decimal(value(row, columns, "교량폭")),
                        value(row, columns, "관리기관명"),
                        date(value(row, columns, "데이터기준일자")));
                loaded.add(inspection);
                inspectionsById.put(catalogId, inspection);
            }
        } catch (IOException e) {
            throw new IllegalStateException("교량 CSV 파일을 읽을 수 없습니다: " + CSV_PATH, e);
        }
        allinspections = List.copyOf(loaded);
    }

    public List<InspectionDto.CatalogInspection> search(String keyword) {
        if (keyword == null || keyword.isBlank())
            return List.of();
        String normalized = keyword.trim().toLowerCase(Locale.ROOT);
        return allinspections.stream()
                .filter(inspection -> inspection.name().toLowerCase(Locale.ROOT).contains(normalized))
                .collect(Collectors.toList());
    }

    public InspectionDto.CatalogInspection getById(String catalogId) {
        InspectionDto.CatalogInspection inspection = inspectionsById.get(catalogId);
        if (inspection == null)
            throw new IllegalArgumentException("CSV에 존재하지 않는 교량 선택값입니다.");
        return inspection;
    }

    private static String value(List<String> row, Map<String, Integer> columns, String name) {
        Integer index = columns.get(name);
        return index == null || index >= row.size() ? "" : row.get(index).trim();
    }

    private static BigDecimal decimal(String value) {
        if (value == null || value.isBlank())
            return null;
        try {
            return new BigDecimal(value.trim());
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private static LocalDate date(String value) {
        if (value == null || value.isBlank())
            return null;
        try {
            return LocalDate.parse(value.trim());
        } catch (Exception ignored) {
            return null;
        }
    }

    // .csv 에 쉼표나 따옴표가 있어도 불러올 수 있도록 작성.
    private static List<String> parseCsvLine(String line) {
        List<String> fields = new ArrayList<>();
        StringBuilder field = new StringBuilder();
        boolean quoted = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') {
                if (quoted && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    field.append('"');
                    i++;
                } else
                    quoted = !quoted;
            } else if (c == ',' && !quoted) {
                fields.add(field.toString());
                field.setLength(0);
            } else
                field.append(c);
        }
        fields.add(field.toString());
        return fields;
    }
}
