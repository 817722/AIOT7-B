package com.aiotb.project.domain.users;

import java.util.ArrayList;
import java.util.List;

import com.aiotb.project.domain.BaseEntity;
import com.aiotb.project.domain.corps.Corp;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// 회원 엔터티
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "users")
public class User extends BaseEntity {

    @Column(name = "username", nullable = false, unique = true, length = 50)
    private String username; // 로그인 ID

    @Column(name = "password", nullable = false)
    private String password; // 비밀번호

    @Column(name = "manager_name", nullable = false, length = 50)
    private String managerName; // 담당자명

    @Column(name = "phone", length = 30)
    private String phone; // 담당자 연락망

    @Builder.Default
    @Column(nullable = false)
    private boolean enabled = true; // 계정 활성화 여부

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "corp_no", nullable = false)
    private Corp corp; // 소속 기업

    // 회원 : 회원 권한 = 1 : N
    @Builder.Default
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<UserAuth> authList = new ArrayList<>();

    // 권한 추가 메소드
    public void addAuth(String auth) {
        UserAuth role = UserAuth.builder()
                .auth(auth)
                .user(this)
                .build();
        this.authList.add(role);
    }

}