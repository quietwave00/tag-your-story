import UserArea from "../user/userArea.js";
import BoardApi from "../board/boardApi.js"

window.onload = () => {
    /**
     * user-area에 대한 처리를 수행한다.
     */
    UserArea.setState();

    const boardId = new URLSearchParams(window.location.search).get('boardId');

    /**
     * 상세 게시글 정보를 요청하고 렌더링한다.
     */
    BoardApi.getBoardByBoardId(boardId).then((response) => {
        renderBoard(response)
    });
};

    // onloadData(`${host}/blog/user/comments/${boardNo}`, showComments);
    // onloadData(`${host}/blog/user/boards/like/count/${boardNo}`, (response) => {
    //     console.log("like check: " + response);
    //     document.getElementById("like-button-area").innerHTML = `<input type = "hidden" id = "checkLiked" value = ${response}>`;
    //     if (response == true) {
    //         document.getElementById("like-button-area").innerHTML += '<span id = "like-button">♥︎</span>';
    //     } else {
    //         document.getElementById("like-button-area").innerHTML += '<span id = "like-button">♡</span>';
    //     }
    // });
    // onloadData(`http://localhost:8080/blog/user/boards/like/${boardNo}`, (response) => {
    //     document.getElementById('like-alert-area').innerHTML = `<span id = "like-alert">${response} Like</span>`;
    // });


/**
 * 상세 게시글 정보를 보여준다. 
 */
const renderBoard = (board) => {
    let content = board.content;
    let nickname = board.nickname;
    let createdAt = board.createdAt;
    let hashtagList = board.hashtagNameList.nameList;

    let hashtagElements = "";
    for(let hashtag of hashtagList) {
        hashtagElements += 
            `
            <div class = "tag-elements" onclick="getBoardListByHashtag(\'${hashtag}\')" data-bs-toggle="modal" data-bs-target="#board-hashtag-modal">#${hashtag}</div>
            `;
    }

    document.getElementById('board-area').innerHTML =
        `
            <div id = "board-element">
                <div class = "row">
                    <div class = "col-9" id = "hashtag-area">${hashtagElements}</div>
                    <div class = "col-3" id = "created-date-area">${createdAt}</div>
                </div>
                
                <div id = "content-area">${content}</div>
                <div id = "user-area">${nickname}</div>
            </div>
        `;
}



//Write Comment
document.getElementById('comment-write-button').addEventListener('click', () => {
    fetch(`${host}/blog/user/comments`, {
        method: "POST",
        headers: {
            "Content-Type": "application/json",
            "Authorization": localStorage.getItem("Authorization")
        },
        body: JSON.stringify({
            "boardNo": new URLSearchParams(window.location.search).get('boardNo'),
            "content": document.getElementById('comment-input').value
        })
    })
    .then((res) => res.json())
    .then(res => {
        if(res.success == true) {
            showComment(res.response);
            document.getElementById('comment-input').value = "";
        }
    })
});

//Show Comment List
const showComments = (comments) => {
    for(let comment of comments) {
        document.getElementById('comment-elements').innerHTML +=
    `
        <div class = "row comment-element">
            <input type = "hidden" class = "commentNo" value = "${comment.commentNo}">
            <div class = "col-2">${comment.userId}</div>
            <div class = "col-4">${comment.content}
                <span class = "replies-button" style = "float: right; color: gray; visibility: hidden;" onclick = "showRepliesForm(${comment.commentNo})">↳</span>
            </div>
            <div class = "col-2 createdDate-area">${comment.createdDate.substr(0, 10)}</div>
        </div>
    `;

        const repliesList = comment.repliesList;
        for(let reply of repliesList) {
            const parentElement = document.querySelector(`.comment-element input[value="${comment.commentNo}"]`).parentElement;
            const repliesDiv = document.createElement('div');
            repliesDiv.classList.add('reply-list');
            repliesDiv.innerHTML = 
            `
                <div class = "row">
                    <div class = "col-2">${reply.userId}</div>
                    <div class = "col-4">${reply.content}
                        <span class = "replies-button" style = "float: right; color: gray; visibility: hidden;" onclick = "showRepliesForm(${comment.commentNo})">↳</span>
                    </div>
                    <div class = "col-2 createdDate-area">${reply.createdDate.substr(0, 10)}</div>
                </div>
            `;
            parentElement.appendChild(repliesDiv);
        }
    }
}

//Show Comment
const showComment = (comment) => {
    console.log("comment when insert reply: " + JSON.stringify(comment));
    document.getElementById('comment-elements').innerHTML +=
    `
        <div class = "row comment-element">
            <input type = "hidden" value = "${comment.commentNo}">
            <div class = "col-2">${comment.userId}</div>
            <div class = "col-4">${comment.content}</div>
            <div class = "col-1"><span class = "replies-button" style = "float: right; color: gray; onclick = "showRepliesForm(${comment.commentNo})">↳</span>
            <div class = "col-2 createdDate-area">${comment.createdDate.substr(0, 10)}</div>
        </div>
    `;

}

//Like Event
document.getElementById('like-button-area').addEventListener('click', function() {
    console.log("클릭했을 때 value값: " + document.getElementById('checkLiked').value);
    if(document.getElementById('checkLiked').value == 'true') { //좋아요했음
        console.log('Liked True');
        fetch(`${host}/blog/user/boards/like/` + boardNo, {
            method: 'DELETE',
            headers: {
                "Content-Type": "application/json",
                "Authorization": localStorage.getItem("Authorization")
            }
        })
        .then((res) => res.json())
        .then(res => {
            if(res.success == true) {
                console.log("UnLike Success");
                document.getElementById('checkLiked').value = false;
                document.getElementById("like-button").innerHTML = "♡";
                onloadData(`http://localhost:8080/blog/user/boards/like/${boardNo}`, (response) => {
                document.getElementById('like-alert-area').innerHTML = `<span id = "like-alert">${response} Like</span>`;
                });
            }
        });
    } else { //좋아요안했음
        fetch(`${host}/blog/user/boards/like`, {
        method: "POST",
        headers: {
            "Content-Type": "application/json",
            "Authorization": localStorage.getItem("Authorization")
        },
        body: JSON.stringify({
            "boardNo": new URLSearchParams(window.location.search).get('boardNo')
        })
    })
    .then((res) => res.json())
    .then(res => {
        if(res.success == true) {
            console.log("Like post Success");
            onloadData(`http://localhost:8080/blog/user/boards/like/count/${boardNo}`, (response) => {
            if (response === true) {
                document.getElementById('checkLiked').value = true;
                document.getElementById("like-button").innerHTML = "♥︎";
                onloadData(`http://localhost:8080/blog/user/boards/like/${boardNo}`, (response) => {
                document.getElementById('like-alert-area').innerHTML = `<span id = "like-alert">${response} Like</span>`;
                });
            }
        });
        }
    })
    }
});


//Replies Event
//Show Reply Form
const showRepliesForm = (commentNo) => {
    const parentElement = document.querySelector(`.comment-element input[value="${commentNo}"]`).parentElement;
    const childDiv = document.createElement('div');
    childDiv.classList.add('child-div');
    childDiv.innerHTML = `<input type = "text" class = "replies-input" placeholder = "Write Reply"><button class = "btn btn-sm btn-dark reply-input-btn">write</button>`;

    parentElement.appendChild(childDiv);

    const btn = childDiv.querySelector('.replies-button');
    btn.addEventListener('click', (e) => {
        const parentNode = e.target.parentNode;
        const replyInput = parentNode.querySelector('input');
        addReplies(commentNo, replyInput);
    });

    
}

//Add Reply
const addReplies = (parentNo, replyInput) => {
    fetch(`${host}/blog/user/comments/replies`, {
        method: 'POST',
        headers: {
            "Content-Type": "application/json",
            "Authorization": localStorage.getItem("Authorization")
        },
        body: JSON.stringify({
            "boardNo": boardNo,
            "parentNo": parentNo,
            "content": replyInput.value
        })
    })
    .then((res) => res.json())
    .then(res => {
        if(res.success == true) {
            console.log("add reply success");
            showReply(parentNo, res.response);
        }
    });
}

//Show Reply
const showReply = (parentNo, reply) => {
    const parentElement = document.querySelector(`.comment-element input[value="${parentNo}"]`).parentElement;
            const repliesDiv = document.createElement('div');
            repliesDiv.classList.add('reply-list');
            repliesDiv.innerHTML = 
            `
                <div class = "row">
                    <div class = "col-1">${reply.userId}</div>
                    <div class = "col-4">${reply.content}
                        <span class = "replies-button" style = "float: right; color: gray; visibility: hidden;" onclick = "showRepliesForm(${parentNo})">↳</span>
                    </div>
                    <div class = "col-2 createdDate-area">${reply.createdDate.substr(0, 10)}</div>
                </div>
            `;
            const elements = document.getElementsByClassName("child-div");
            for (let i = 0; i < elements.length; i++) {
                elements[i].style.display = "none";
            }
            parentElement.appendChild(repliesDiv);
}

//Hashtag Event
const getBoardListByHashtag = (tag) => {
    fetch(`${host}/blog/user/hashtag/` + tag, {
        method: "GET",
        headers: {
            "Content-Type": "application/json",
            "Authorization": localStorage.getItem("Authorization")
        }
    })
    .then((res) => res.json())
    .then((res) => {
        if (res.success === true) {
            showBoardListByHashtag(res.response);
        }
    });
}

const showBoardListByHashtag = (boardList) => {
    document.getElementById('board-element-area').innerHTML = "";
    for(let board of boardList) {
        document.getElementById('board-element-area').innerHTML += 
            `
            <div class = "board-element">
                <div class = "row">
                    <div>
                        <input type = "hidden" class = "board-no" value = "${board.boardNo}">
                        <div class = "content-area">${board.content}</div>
                        <div class = "nickname-area">${board.nickname}</div>
                    </div>
                </div>
            </div>
            `;
    }
    moveDetails();
}

//Move To The Board
const moveDetails = () => {
    console.log("move to board Called");
    const boardElements = document.getElementsByClassName('board-element');

    for(let element of boardElements) {
        element.addEventListener('click', (e) => {
            const boardNo = e.currentTarget.querySelector('.board-no').value;
            console.log("select boardNo: " + boardNo);
            window.location.href = `board.html?boardNo=${boardNo}`;
        })
    }
}
