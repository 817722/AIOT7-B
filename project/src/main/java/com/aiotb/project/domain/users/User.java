package com.aiotb.project.domain.users;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.aiotb.project.domain.BaseEntity;
import com.aiotb.project.domain.corps.Corp;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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

/**
 * 실제 DB users 테이블과 매핑.
 * - 기존 enabled 컬럼은 DB에 존재하지 않으므로 status(PENDING/ACTIVE/REJECTED/DISABLED)로 대체.
 * - 로그인 허용 = users.status = ACTIVE AND corps.status = ACTIVE
 */
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
@Entity
@Table(name = "users")
public class User extends BaseEntity {

    /** 사용자 계정 상태 (기업 ADMIN이 승인·반려·비활성 처리) */
    public enum Status { PENDING, ACTIVE, REJECTED, DISABLED }

    @Column(name = "username", nullable = false, unique = true, length = 100)
    private String username;

    @Column(name = "password", nullable = false)
    private String password; // 비밀번호

    @Column(name = "manager_name", nullable = false, length = 100)
    private String managerName; // 담당자명

    @Column(name = "phone", nullable = false, length = 100)
    private String phone; // 담당자 연락망

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private Status status = Status.PENDING;

    @Column(name = "status_by")
    private Long statusBy;           // 승인·반려·비활성 처리한 같은 기업 ADMIN 사용자 번호

    @Column(name = "status_at")
    private LocalDateTime statusAt;  // 처리 일시

    @Column(name = "reject", length = 500)
    private String reject;           // 반려·비활성 사유

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "corp_no", nullable = false)
    private Corp corp;

    @Builder.Default
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<UserAuth> authList = new ArrayList<>();

    public void addAuth(String roleName) {
        UserAuth role = UserAuth.builder()
                .auth(UserAuth.Role.valueOf(roleName))
                .user(this)
                .build();
        this.authList.add(role);
    }

    public boolean hasRole(UserAuth.Role role) {
        return authList.stream().anyMatch(auth -> auth.getAuth() == role);
    }

    /** 서비스 이용 가능 여부: 사용자와 소속 기업이 모두 ACTIVE 상태여야 한다. */
    public boolean canUseService() {
        return status == Status.ACTIVE && corp != null && corp.getStatus() == Corp.Status.ACTIVE;
    }
}
