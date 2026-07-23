package site.dataon.hyeyum.dto;

public record KsicIndustrySearchItem(
        String ksicCode,
        String sectionCode,
        String sectionName,
        String divisionCode,
        String divisionName,
        String groupCode,
        String groupName,
        String classCode,
        String className,
        String subclassCode,
        String subclassName,
        String displayName) {}
