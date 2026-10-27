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

@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
@Entity
@Table(name = "users")
public class User extends BaseEntity {
    @Column(nullable = false, unique = true, length = 100)
    private String username;

    @Column(nullable = false, length = 255)
    private String password;

    // Java 속성 name 유지, 최종 DDL의 manager_name 컬럼에 연결
    @Column(name = "manager_name", nullable = false, length = 100)
    private String name;

    @Column(nullable = false, length = 100)
    private String phone;

    @Builder.Default
    @Column(nullable = false)
    private boolean enabled = true;

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
}
