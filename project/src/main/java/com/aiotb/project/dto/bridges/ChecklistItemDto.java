package com.aiotb.project.dto.bridges;

import lombok.Getter;
import lombok.Setter;

@Getter 
@Setter 
public class ChecklistItemDto {
    private String selectValue; // <td><select>에서 선택된 값
    private String inputText;   // <td><input type="text">에 입력된 값
}
