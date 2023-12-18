import UserArea from "../user/userArea.js";
import TrackApi from "./trackApi.js";
import BoardApi from "../board/boardApi.js";
import File from "../board/file.js";
import FileApi from "../board/fileApi.js";
import Board from "../board/board.js";
import { trackManager } from "./trackManager.js";
import { eventSource } from '../notification/notificationManager.js'
import { renderNotification } from '../notification/notificationManager.js';

window.onload = async () => {
    /**
     * user-area에 대한 처리를 수행한다.
     */
    UserArea.setState();

    /**
     * 실시간 알림을 수행한다.
     */
    if(eventSource) {
        eventSource.addEventListener('Notification', (e) => {
            renderNotification(e.data);
        });
    }

    const trackId = new URLSearchParams(window.location.search).get('trackId');
    const defaultPage = 1;
    const defaultOrderType = "CREATED_AT";
    /**
     * 트랙의 상세 정보를 가져온다.
     */
    TrackApi.getDetailTrackById(trackId).then((response) => {
        renderDetailTrack(response)
    });

    /**
     * 게시물 리스트를 가져온다.
     */
    await BoardApi.getBoardListByTrackId(trackId, defaultOrderType, defaultPage).then((response) => {
        Board.renderBoardList(response)
    });
    

    /**
     * 메인 이미지 파일을 가져온다.
     */
    await FileApi.getMainFileList(trackId, defaultPage).then((response) => {
        File.renderMainFileList(response)
    });
};

/**
 * 트랙의 상세 정보를 보여준다.
 */
const renderDetailTrack = (track) => {
    let title = track.title;
    let artist = track.artistName;
    let album = track.albumName;
    let imageUrl = track.imageUrl;

    document.getElementById('track-area').innerHTML += 
            `
                <div class = "row track">
                    <div class = "col-5">
                        <img src = "${imageUrl}" style = "width: 60%; margin-left: 100px;">
                    </div>
                    <div class = "col-7">
                        <div class = "row">
                            <div class = "col-7">
                                <span class = "title">${title}</span>
                            </div>
                            <div class = "col-7">
                                <span style = "color: gray; font-weight: bold;">Artist</span> <span class = "artist">${artist}</span>
                            </div>
                            <div class = "col-7">
                                <span style = "color: gray; font-weight: bold;">Album</span> <span class = "album">${album}</span>
                            </div>
                            <div class = "col-7">
                                <input type = "button" class="btn btn-dark" value = "write" id = "modal-button" data-bs-toggle="modal" data-bs-target="#boardModal">
                            </div>
                        </div>
                    </div>        
                </div> 
            `;
}

/**
 * 뒤로가기 버튼을 처리한다.
 */
document.getElementById('back-area').addEventListener('click', () => {
    const keyword = trackManager.getTrackInfo().selectedKeyword;
    const page = trackManager.getTrackInfo().selectedPage;
    window.location.href = `${client_host}/tracks.html?keyword=${keyword}&page=${page}`;
});