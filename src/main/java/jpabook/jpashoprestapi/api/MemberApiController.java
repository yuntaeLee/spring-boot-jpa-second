package jpabook.jpashoprestapi.api;

import jpabook.jpashoprestapi.domain.Member;
import jpabook.jpashoprestapi.service.MemberService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

@RestController
@RequiredArgsConstructor
public class MemberApiController {

    private final MemberService memberService;

    @PostMapping("api/v1/members")
    public CreatememberResponse saveMemberV1(@RequestBody @Valid Member member) {
        Long id = memberService.join(member);
        return new CreatememberResponse(id);
    }

    @Data
    static class CreatememberResponse {
        private Long id;

        public CreatememberResponse(Long id) {
            this.id = id;
        }
    }
}
