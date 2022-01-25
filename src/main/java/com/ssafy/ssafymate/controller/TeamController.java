package com.ssafy.ssafymate.controller;

import com.ssafy.ssafymate.JWT.TokenProvider;
import com.ssafy.ssafymate.common.BaseResponseBody;
import com.ssafy.ssafymate.dto.request.TeamListReuestDto;
import com.ssafy.ssafymate.dto.request.TeamRequestDto;
import com.ssafy.ssafymate.dto.response.BelongToTeam;
import com.ssafy.ssafymate.dto.response.TeamListResponseDto;
import com.ssafy.ssafymate.dto.response.TeamResponseDto;
import com.ssafy.ssafymate.entity.Team;
import com.ssafy.ssafymate.entity.TeamStack;
import com.ssafy.ssafymate.entity.User;
import com.ssafy.ssafymate.service.TeamService;
import com.ssafy.ssafymate.service.UserService;
import com.ssafy.ssafymate.service.UserTeamService;
import io.swagger.annotations.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.stream.Collectors;

@Api(value = "팀 API", tags = {"Team"})
@RestController
@RequestMapping("/api/auth/team")
public class TeamController {

    @Autowired
    TeamService teamService;

    @Autowired
    UserService userService;

    @Autowired
    UserTeamService userTeamService;

    @Autowired
    private TokenProvider tokenProvider;

    @GetMapping("/{teamId}")
    @ApiOperation(value = "팀 상세조회", notes = "팀 아이디로 해당 팀 상세 조회")
    @ApiResponses({
            @ApiResponse(code = 200, message = "성공"),
            @ApiResponse(code = 400, message = "인증 실패"),
            @ApiResponse(code = 500, message = "서버 오류")
    })
    public ResponseEntity<?> findTeam(
            @PathVariable final Long teamId
    ){
        Team teamdata;

        try {
        teamdata = teamService.teamfind(teamId).orElse(null);
        if(teamdata==null){
            return ResponseEntity.status(400).body(BaseResponseBody.of(400, false,  "팀 정보 없음"));
        }

        }catch (Exception exception){
            return ResponseEntity.status(500).body(BaseResponseBody.of(500, false,  "Internal Server Error, 팀 상세 정보 조회 실패"));
        }
        return ResponseEntity.status(200).body(TeamResponseDto.of(200, true,  "success",teamdata));
    }

    @GetMapping("/userIn")
    @ApiOperation(value = "팀 참여 여부 조회", notes = "유저 아이디와 선택한 프로젝트로 해당 프로젝트에서 이미 팀에 참여 했는지 여부를 조회")
    @ApiResponses({
            @ApiResponse(code = 200, message = "성공"),
            @ApiResponse(code = 500, message = "서버 오류")
    })
    public ResponseEntity<?extends BaseResponseBody> canCreateTeam(
            @RequestParam final Long userId,
            @RequestParam final String selectedProject){

        boolean belongToTeam = false;

        try {
                Team team = teamService.belongToTeam(selectedProject,userId).orElse(null);
                if(team == null){
                    belongToTeam = true;
                }
        }catch (Exception exception){
            return ResponseEntity.status(500).body(BaseResponseBody.of(500, false,  ""));
        }

        return ResponseEntity.status(200).body(BelongToTeam.of(200, true,  "success",belongToTeam));
    }

    @PostMapping("/")
    @ApiOperation(value = "팀 생성", notes = "작성된 팀 정보와 유저 아이디를 가지고 팀생성")
    @ApiResponses({
            @ApiResponse(code = 200, message = "성공"),
            @ApiResponse(code = 400, message = "인증 실패"),
            @ApiResponse(code = 401, message = "인증 실패"),
            @ApiResponse(code = 500, message = "서버 오류")
    })
    public ResponseEntity<? extends BaseResponseBody> createTeam(
                @RequestPart(value= "teamRequestDto")TeamRequestDto teamRequestDto,
            @RequestPart(value= "file", required = false) MultipartFile multipartFile,
                @RequestPart(value = "userId") String email,
                @AuthenticationPrincipal String token)  {
        User user = userService.getUserByEmail(email);
        if (user == null) {
            return ResponseEntity.status(401).body(BaseResponseBody.of(401, false, "사용자 정보가 없습니다."));
        }
        try {
            if(user.getEmail().equals(token)) {
                return ResponseEntity.status(400).body(BaseResponseBody.of(400, false, "접근 권한 없음"));
            }
            Team team = teamService.teamSave(teamRequestDto, multipartFile,user);
            userTeamService.userTamSave(user,team);
        } catch (Exception exception) {
            return ResponseEntity.status(500).body(BaseResponseBody.of(500, false,  "Internal Server, 팀 생성 실패"));
        }
        return ResponseEntity.status(200).body(BaseResponseBody.of(200, true,  "success"));
    }

    @PutMapping("/{teamId}")
    @ApiOperation(value = "팀 수정", notes = "수정된 팀 정보를 가지고 팀 수정")
    @ApiResponses({
            @ApiResponse(code = 200, message = "성공"),
            @ApiResponse(code = 400, message = "인증 실패"),
            @ApiResponse(code = 500, message = "서버 오류")
    })
    public ResponseEntity<? extends BaseResponseBody> modifyTeam(
            @RequestPart(value= "teamRequestDto")TeamRequestDto teamRequestDto,
            @RequestPart(value= "file", required = false) MultipartFile multipartFile,
            @RequestPart(value = "userId") Long userId,
            @PathVariable final Long teamId,
            @AuthenticationPrincipal String token) {

        try {
            Team team = teamService.ownTeam(teamId,userId).orElse(null);
            if(team==null || !team.getOwner().getEmail().equals(token)){
                return ResponseEntity.status(400).body(BaseResponseBody.of(400, false,  "팀 수정 권한이 없습니다."));
            }
            User user = userService.getUserById(userId);
            teamService.teamModify(teamRequestDto, multipartFile, user, teamId);
        } catch (Exception exception) {
            return ResponseEntity.status(500).body(BaseResponseBody.of(500, false,  "Internal Server, 팀 상세 정보 수정 실패"));
        }
        return ResponseEntity.status(200).body(BaseResponseBody.of(200, true,  "success"));
    }

    @DeleteMapping("/{teamId}")
    @ApiOperation(value = "팀 삭제", notes = "팀장이 팀 삭제")
    @ApiResponses({
            @ApiResponse(code = 200, message = "성공"),
            @ApiResponse(code = 400, message = "인증 실패"),
            @ApiResponse(code = 500, message = "서버 오류")
    })
    public ResponseEntity<? extends BaseResponseBody> deleteTeam(
            @PathVariable final Long teamId,
            @RequestParam final Long userId,
            @AuthenticationPrincipal String token
    ){
        try {

            Team team = teamService.ownTeam(teamId, userId).orElse(null);
            if(team==null || !team.getOwner().getEmail().equals(token)){
                return ResponseEntity.status(400).body(BaseResponseBody.of(400, false,  "팀 삭제에 권한이 없습니다."));
            }
            teamService.teamDelete(teamId);
        } catch (Exception exception) {
            return ResponseEntity.status(500).body(BaseResponseBody.of(500, false,  "팀 삭제에 실패하였습니다."));
        }
        return ResponseEntity.status(200).body(BaseResponseBody.of(200, true,  "success"));
    }

    @PostMapping("/teamList")
    @ApiOperation(value = "팀 리스트 조회", notes = "프로젝트, 프로젝트 트랙, 기술스택을 가지고 팀 리스트 조회")
    @ApiResponses({
            @ApiResponse(code = 200, message = "성공"),
            @ApiResponse(code = 400, message = "인증 실패"),
            @ApiResponse(code = 500, message = "서버 오류")
    })
    public ResponseEntity<? extends BaseResponseBody> SearchTeam(
            @RequestBody TeamListReuestDto teamListReuestDto
            ){
        List<Team> teams;
        try {
            if((teamListReuestDto.getTechStacks().size() == 0) || (teamListReuestDto.getTechStacks()==null)) {
                System.out.println("not stack");
                teams = teamService.teamSearch(teamListReuestDto.getProject(), teamListReuestDto.getProjectTrack(), teamListReuestDto.getTeamName()).orElse(null);
            }
            else{
                System.out.println("stack");
                List<String> teamstacks = teamListReuestDto.getTechStacks().stream().map(e -> e.getTechStackName()).collect(Collectors.toList());
                teams = teamService.teamSearch(teamListReuestDto.getProject(), teamListReuestDto.getProjectTrack(), teamListReuestDto.getTeamName() ,teamstacks ).orElse(null);
            }
        }catch (Exception exception){
            return ResponseEntity.status(400).body(BaseResponseBody.of(400, false,  "팀 조회에 실패하였습니다.."));
        }


        return ResponseEntity.status(200).body(TeamListResponseDto.of(200, true,  "success",teams));

    }
}
