import { useEffect, useMemo, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { useNavigate } from 'react-router-dom';
import { userTagService } from '../services/userTagService.js';
import { trackService } from '../services/trackService.js';
import { authStore } from '../store/authStore.js';
import { buildRoute, routes } from '../utils/routes.js';
import { saveTrackSearch } from '../utils/trackSearchStorage.js';
import { tokenStorage } from '../utils/tokenStorage.js';
import logoUrl from '../../assets/tagnote-logo-transparent.png';
import '../styles/home.css';

const DEFAULT_PAGE = 1;

function normalizeKeywordList(rankingResponse) {
  return rankingResponse?.keywordList ?? [];
}

export default function HomePage() {
  const { t } = useTranslation('home');
  const navigate = useNavigate();
  const [keyword, setKeyword] = useState('');
  const [userTags, setUserTags] = useState([]);
  const [rankingKeywords, setRankingKeywords] = useState([]);
  const [isLoadingUserTags, setIsLoadingUserTags] = useState(true);
  const [isLoadingRanking, setIsLoadingRanking] = useState(true);
  const [userTagError, setUserTagError] = useState(null);
  const [rankingError, setRankingError] = useState(null);

  const trimmedKeyword = useMemo(() => keyword.trim(), [keyword]);

  useEffect(() => {
    if (tokenStorage.discardLegacyPending() && !authStore.getState().isAuthenticated) {
      navigate(routes.login, { replace: true, state: { messageKey: 'oauth.legacyPending' } });
    }
  }, [navigate]);

  useEffect(() => {
    let ignore = false;

    async function loadRecentUserTags() {
      try {
        const response = await userTagService.getRecentUserTagList();

        if (!ignore) {
          setUserTags(response ?? []);
          setUserTagError(null);
        }
      } catch (error) {
        if (!ignore) {
          setUserTagError(error);
        }
      } finally {
        if (!ignore) {
          setIsLoadingUserTags(false);
        }
      }
    }

    loadRecentUserTags();

    return () => {
      ignore = true;
    };
  }, []);

  useEffect(() => {
    let ignore = false;

    async function loadRankingKeywords() {
      try {
        const response = await trackService.getKeywordRanking();

        if (!ignore) {
          setRankingKeywords(normalizeKeywordList(response));
          setRankingError(null);
        }
      } catch (error) {
        if (!ignore) {
          setRankingError(error);
        }
      } finally {
        if (!ignore) {
          setIsLoadingRanking(false);
        }
      }
    }

    loadRankingKeywords();

    return () => {
      ignore = true;
    };
  }, []);

  const moveToSearchResult = (nextKeyword) => {
    const nextTrimmedKeyword = nextKeyword.trim();

    if (!nextTrimmedKeyword) {
      return;
    }

    saveTrackSearch({ keyword: nextTrimmedKeyword, page: DEFAULT_PAGE });
    navigate(`${routes.tracks}?keyword=${encodeURIComponent(nextTrimmedKeyword)}&page=${DEFAULT_PAGE}`);
  };

  const handleSubmit = (event) => {
    event.preventDefault();
    moveToSearchResult(trimmedKeyword);
  };

  const handleUserTagClick = (boardId) => {
    navigate(buildRoute.boardDetail(boardId));
  };

  const cloudKeywords = rankingKeywords.length > 0
    ? rankingKeywords
    : ['shoegaze', 'dream pop', 'post punk', 'ambient', 'noise', 'jangle', 'slowcore'];

  return (
    <div className="home-page">
      <section className="home-hero" aria-label={t('search.label')}>
        <img aria-hidden="true" className="home-logo" src={logoUrl} alt="" />
        <form className="track-search-form" onSubmit={handleSubmit}>
          <input
            aria-label={t('search.label')}
            className="track-search-input"
            onChange={(event) => setKeyword(event.target.value)}
            placeholder={t('search.placeholder')}
            type="search"
            value={keyword}
          />
          <button className="track-search-button" disabled={!trimmedKeyword} type="submit">
            {t('search.submit')}
          </button>
        </form>
      </section>

      <section className="home-section tag-cloud-section" aria-labelledby="ranking-title">
        <h2 className="home-section-title" id="ranking-title">
          {t('ranking.title')}
        </h2>
        {isLoadingRanking ? <p className="home-message">{t('ranking.loading')}</p> : null}
        {rankingError ? <p className="home-message error">{t('ranking.error')}</p> : null}
        <div className="keyword-cloud">
          {cloudKeywords.map((rankingKeyword, index) => (
            <button
              className="keyword-cloud-item"
              key={`${rankingKeyword}-${index}`}
              onClick={() => moveToSearchResult(rankingKeyword)}
              style={{ '--keyword-rank': index }}
              type="button"
            >
              #{rankingKeyword}
            </button>
          ))}
        </div>
      </section>

      <section className="home-section" aria-labelledby="recent-user-tag-title">
        <h2 className="home-section-title" id="recent-user-tag-title">
          {t('recentTags.title')}
        </h2>
        {isLoadingUserTags ? <p className="home-message">{t('recentTags.loading')}</p> : null}
        {userTagError ? (
          <p className="home-message error">{t('recentTags.error')}</p>
        ) : null}
        {!isLoadingUserTags && !userTagError && userTags.length === 0 ? (
          <p className="home-message">{t('recentTags.empty')}</p>
        ) : null}
        <div className="user-tag-list">
          {userTags.map(({ boardId, userTag }, index) => (
            <article className="archive-stream-item" key={`${boardId}-${userTag?.name}`}>
              <span className="archive-index">{String(index + 1).padStart(2, '0')}</span>
              <button
                className="user-tag-button"
                onClick={() => handleUserTagClick(boardId)}
                type="button"
              >
                #{userTag?.name}
              </button>
            </article>
          ))}
        </div>
      </section>
    </div>
  );
}
