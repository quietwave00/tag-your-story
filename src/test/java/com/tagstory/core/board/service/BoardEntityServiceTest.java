package com.tagstory.core.board.service;

import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class BoardEntityServiceTest {
//
//    @Mock
//    private BoardRepository boardRepository;
//
//    @InjectMocks
//    private BoardService boardService;
//
//    private UserEntity userEntity;
//
//    @BeforeEach
//    void init() {
//        userEntity = UserEntity.builder()
//                .userId(1L)
//                .nickname("test")
//                .build();
//
//    }

//    @Test
//    @DisplayName("글 작성")
//    void create() {
//        //given
//
//        HashtagEntity hashtagEntity = HashtagEntity.builder()
//                .hashtagId(1L)
//                .name("test")
//                .build();
//        List<HashtagEntity> hashtagEntityList = List.of(hashtagEntity);
//
//        CreateBoardCommand createBoardCommand = CreateBoardCommand.builder()
//                .hashtagList(List.of("test"))
//                .content("test")
//                .build();
//
//        BoardEntity boardEntity = BoardEntity.create(createBoardCommand);
//        boardEntity.addUser(userEntity);
////        boardEntity.addHashtag(hashtagEntityList);
//
//        //when
//        when(boardRepository.save(any())).thenReturn(boardEntity);
//        Board board = boardService.create(createBoardCommand, userEntity.toUser(), hashtagEntityList);
//
//        //then
//        assertThat(createBoard.getNickname()).isEqualTo(userEntity.getNickname());
//        assertThat(createBoard.getContent()).isEqualTo(boardEntity.getContent());
//        assertThat(createBoard.getHashtagList().get(0)).isEqualTo(hashtagEntity.getName());
//}

//    @Test
//    @DisplayName("트랙에 따른 게시글 리스트 조회")
//    void getBoardListByTrackId() {
//        //given
//        String trackId = "test";
//        BoardEntity boardEntity1 = new BoardEntity();
//        boardEntity1.addUser(userEntity);
//        boardEntity1.generateTestBoard("test1", "content1", "nickname1");
//        BoardEntity boardEntity2 = new BoardEntity();
//        boardEntity2.addUser(userEntity);
//        boardEntity2.generateTestBoard("test2", "content2", "nickname2");
//        List<BoardEntity> boardEntityList = List.of(boardEntity1, boardEntity2);
//
//        //when
//        when(boardRepository.findByStatusAndTrackIdOrderByBoardIdDesc(any(), anyString(), any(Pageable.class)))
//                .thenReturn(new PageImpl<>(boardEntityList));
//        List<BoardByTrack> result = boardService.getBoardListByTrackId(trackId, 0);
//
//        //then
//        assertThat(result.size()).isEqualTo(2);
//        assertThat(result.get(0).getContent()).isEqualTo("content1");
//        assertThat(result.get(1).getContent()).isEqualTo("content2");
}
