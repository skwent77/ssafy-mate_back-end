package com.ssafy.ssafymate.dto.response;

import com.ssafy.ssafymate.dto.ChatDto.ContentList;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@ApiModel("ChatHistoryResponseDto")
public class ChatHistoryResponseDto {

    @ApiModelProperty(name = "대화내용 리스트", example = "contentLists: []")
    List<ContentList> contentList;

    public static ChatHistoryResponseDto of(List<ContentList> contentList) {
        ChatHistoryResponseDto res = new ChatHistoryResponseDto();
        res.setContentList(contentList);
        return res;
    }
}