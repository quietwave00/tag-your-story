# ADR-002 — User-Owned Custom Tags

## Decision

개인 커스텀 `UserTag`는 전역 공유 사전이 아니라 사용자별 태그 사전으로 관리한다.

- `user_tag`는 소유자인 `user_id`를 필수 FK로 가진다.
- 원본 표시 이름 `name`과 조회·중복 판정용 `normalized_name`을 분리한다.
- 태그 identity와 중복 방지 기준은 `(user_id, normalized_name)`이다.
- 같은 사용자가 같은 normalized name을 반복 사용하면 기존 `user_tag_id`를 재사용한다.
- 서로 다른 사용자가 같은 normalized name을 사용하면 서로 다른 `user_tag_id`를 생성한다.
- 게시글에 연결되는 UserTag의 소유자는 반드시 게시글 작성자와 같아야 한다.
- 전체 커뮤니티의 태그명 기반 게시글 조회는 단일 `user_tag_id`를 먼저 선택하지 않고 `normalized_name`으로 모든 사용자의 태그를 조회한다.
- System Tag의 `tag`, `tag_alias`, `subject_tag_resolved`와 개인 `user_tag`는 Entity, Repository, Service, 테이블 및 identity 정책을 공유하지 않는다.

DB의 최종 정합성 제약은 다음과 같다.

```text
FK(user_tag.user_id → users.user_id)
UNIQUE(user_tag.user_id, user_tag.normalized_name)
UNIQUE(board_user_tag.board_id, board_user_tag.user_tag_id)
```

## Reason

사용자가 직접 작성하는 `"1월"`, `"출근길"`, `"내 인생곡"` 같은 태그는 공용 taxonomy가 아니라 개인의 분류 체계다. 이름이 같다는 이유만으로 서로 다른 사용자가 하나의 `user_tag_id`를 공유하면 소유권을 표현할 수 없고, 개인별 태그 목록·수정·삭제 정책을 독립적으로 발전시키기 어렵다.

반대로 같은 사용자가 동일한 의미의 태그를 게시글마다 새 row로 만들면 개인 태그별 조회와 재사용이 불가능해지고 중복 데이터가 증가한다. 따라서 사용자 내부에서는 normalized name으로 재사용하고 사용자 사이에서는 identity를 분리한다.

공개 게시글을 태그명으로 탐색하는 기능은 개인 identity와 별개의 조회 요구다. 사용자별 ID가 여러 개이므로 `name → 단일 tag ID → Board 조회` 구조가 아니라 normalized name을 기준으로 `user_tag`와 `board_user_tag`를 직접 join해야 한다.

## Consequence

- 기존 전역 `findByName`, `findAllByNameIn` 조회는 owner-scoped 조회로 교체한다.
- Board 생성·수정은 작성자 ID를 UserTag find-or-create에 전달해야 한다.
- 같은 이름을 가진 여러 UserTag가 정상 데이터가 되므로 `Optional<UserTagEntity> findByName(...)`는 사용할 수 없다.
- 기존 공개 `GET /api/boards/user-tags?userTagName=...` contract는 유지하되, 내부 조회는 normalized name에 해당하는 모든 사용자별 tag ID를 포함해야 한다.
- 기존 전역 공유 데이터는 Board 작성자를 기준으로 사용자별 UserTag row를 생성하고 `board_user_tag`를 재연결하는 migration이 필요하다.
- 사용자별 태그 목록 및 관리 API는 이 identity를 사용할 수 있지만, `USER-TAG-001`에서는 기존 게시글 생성·수정·조회 동작에 필요한 범위만 구현한다.
- System Tag의 전역 canonical identity와 unique 정책에는 영향이 없다.
