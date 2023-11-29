/* 트랙의 상태 관리를 도와준다. */
const trackManager = (() => {
    let selectedTrackId;
    let selectedTitle;
    let selectedKeyword;
    let selectedPage = 1;

    const saveTrackId = (trackId) => {
        sessionStorage.setItem("trackId", trackId);
    };

    const saveTitle = (title) => {
        sessionStorage.setItem("title", title);
    };

    const saveKeyword = (keyword) => {
        sessionStorage.setItem("keyword", keyword);
    };

    const savePage = (page) => {
        sessionStorage.setItem("page", page);
    };

    const setUp = () => {
        selectedTrackId = sessionStorage.getItem("trackId");
        selectedTitle = sessionStorage.getItem("title");
        selectedKeyword = sessionStorage.getItem("keyword");
        selectedPage = sessionStorage.getItem("page");
    }
  
    return {
      setTrackId: (trackId) => {
        saveTrackId(trackId);
      },
      setTitle: (title) => {
        saveTitle(title);
      },
      setKeyword: (keyword) => {
        saveKeyword(keyword);
      },
      setPage: (page) => {
        savePage(page);
      },
      getTrackInfo: () => {
        setUp();
        return {
          selectedTrackId,
          selectedTitle,
          selectedKeyword,
          selectedPage
        };
      },
    };
  })();

export {
    trackManager
};
