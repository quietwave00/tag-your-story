import { useEffect, useMemo, useState } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { trackService } from '../services/trackService.js';
import { buildRoute } from '../utils/routes.js';
import { saveSelectedTrack, saveTrackSearch } from '../utils/trackSearchStorage.js';
import '../styles/tracks.css';

const PAGE_SIZE = 10;
const DEFAULT_PAGE = 1;

function getValidPage(pageParam) {
  const page = Number(pageParam);

  return Number.isInteger(page) && page > 0 ? page : DEFAULT_PAGE;
}

function getPageNumbers(totalPages) {
  return Array.from({ length: totalPages }, (_, index) => index + 1);
}

export default function TracksPage() {
  const navigate = useNavigate();
  const [searchParams, setSearchParams] = useSearchParams();
  const keyword = searchParams.get('keyword')?.trim() ?? '';
  const currentPage = getValidPage(searchParams.get('page'));
  const [tracks, setTracks] = useState([]);
  const [totalCount, setTotalCount] = useState(0);
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState(null);

  const totalPages = useMemo(() => {
    const calculatedTotalPages = Math.ceil(totalCount / PAGE_SIZE);

    return calculatedTotalPages < 1 ? 1 : calculatedTotalPages;
  }, [totalCount]);

  const pageNumbers = useMemo(() => getPageNumbers(totalPages), [totalPages]);

  useEffect(() => {
    if (!keyword) {
      setTracks([]);
      setTotalCount(0);
      setError(null);
      return;
    }

    let ignore = false;

    async function searchTracks() {
      setIsLoading(true);

      try {
        const response = await trackService.searchTrack(keyword, currentPage);

        if (!ignore) {
          setTracks(response?.trackDataList ?? []);
          setTotalCount(response?.totalCount ?? 0);
          setError(null);
          saveTrackSearch({ keyword, page: currentPage });
        }
      } catch (requestError) {
        if (!ignore) {
          setTracks([]);
          setTotalCount(0);
          setError(requestError);
        }
      } finally {
        if (!ignore) {
          setIsLoading(false);
        }
      }
    }

    searchTracks();

    return () => {
      ignore = true;
    };
  }, [currentPage, keyword]);

  const moveToPage = (page) => {
    if (!keyword || page < 1 || page > totalPages || page === currentPage) {
      return;
    }

    setSearchParams({ keyword, page: String(page) });
    window.scrollTo({ top: 0, behavior: 'smooth' });
  };

  const handleTrackClick = (track) => {
    saveSelectedTrack({ trackId: track.trackId, title: track.title });
    navigate(buildRoute.trackDetail(track.trackId));
  };

  if (!keyword) {
    return (
      <section className="tracks-page">
        <header className="tracks-header">
          <h1 className="tracks-title">검색</h1>
          <p className="tracks-description">홈에서 트랙, 앨범, 아티스트를 검색해 주세요.</p>
        </header>
      </section>
    );
  }

  return (
    <section className="tracks-page">
      <header className="tracks-header">
        <h1 className="tracks-title">검색 결과</h1>
        <p className="tracks-description">
          <strong>{keyword}</strong>
        </p>
      </header>

      {isLoading ? <p className="tracks-message">트랙을 불러오는 중입니다 █</p> : null}
      {error ? <p className="tracks-message error">트랙을 불러오지 못했습니다</p> : null}
      {!isLoading && !error && tracks.length === 0 ? (
        <p className="tracks-message">검색 결과가 없습니다</p>
      ) : null}

      <div className="track-list">
        {tracks.map((track, index) => (
          <button
            className="track-card"
            key={track.trackId}
            onClick={() => handleTrackClick(track)}
            type="button"
          >
            <span className="track-row-index">{String(index + 1).padStart(2, '0')}</span>
            <img className="track-image" src={track.imageUrl} alt={`${track.title} album cover`} />
            <span className="track-content">
              <span className="track-title">{track.title}</span>
              <span className="track-meta">
                <span className="track-meta-row">
                  <span className="track-meta-label">Artist</span>
                  <span className="track-meta-value">{track.artistName}</span>
                </span>
                <span className="track-meta-row">
                  <span className="track-meta-label">Album</span>
                  <span className="track-meta-value">{track.albumName}</span>
                </span>
                <span className="track-meta-row">
                  <span className="track-meta-label">Tag</span>
                  <span className="track-meta-value">#{keyword}</span>
                </span>
              </span>
            </span>
          </button>
        ))}
      </div>

      {!isLoading && !error && tracks.length > 0 ? (
        <nav className="tracks-pagination" aria-label="트랙 검색 결과 페이지">
          <button
            aria-label="이전 페이지"
            className="pagination-button"
            disabled={currentPage <= 1}
            onClick={() => moveToPage(currentPage - 1)}
            type="button"
          >
            ⤺
          </button>
          {pageNumbers.map((page) => (
            <button
              aria-current={page === currentPage ? 'page' : undefined}
              className={page === currentPage ? 'pagination-number active' : 'pagination-number'}
              key={page}
              onClick={() => moveToPage(page)}
              type="button"
            >
              {page}
            </button>
          ))}
          <button
            aria-label="다음 페이지"
            className="pagination-button"
            disabled={currentPage >= totalPages}
            onClick={() => moveToPage(currentPage + 1)}
            type="button"
          >
            ⤻
          </button>
        </nav>
      ) : null}
    </section>
  );
}
