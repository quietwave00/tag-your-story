import { useTranslation } from 'react-i18next';
import '../../styles/subjectTags.css';

export default function SubjectTags({ tags, loading, error, hero = false }) {
  const { t } = useTranslation('tracks');
  const status = tags?.tagDisplayStatus;
  const items = status === 'CONFIRMED' ? tags.systemTags : status === 'PREVIEW' ? tags.previewTags : [];

  return (
    <section aria-label={t('subjectTags.title')} className={hero ? 'subject-tags subject-tags--hero' : 'subject-tags'}>
      {!hero ? <h2>{t('subjectTags.title')}</h2> : null}
      {loading ? (
        <div aria-busy="true" className="subject-tags-skeleton" role="status">
          <span className="visually-hidden">{t('subjectTags.loading')}</span>
          <span aria-hidden="true" className="skeleton-block" />
          <span aria-hidden="true" className="skeleton-block" />
        </div>
      ) : null}
      {error ? <p role="alert">{t('subjectTags.error')}</p> : null}
      {!loading && !error && status === 'PREVIEW' ? <p>{t('subjectTags.preview')}</p> : null}
      {!loading && !error && status === 'EMPTY' ? <p>{t('subjectTags.empty')}</p> : null}
      {!loading && !error && items?.length > 0 ? (
        <ul>
          {items.map((item) => <li key={status === 'CONFIRMED' ? item.tagId : `${item.name}:${item.source}`}>{hero ? `#${item.name}` : item.name}</li>)}
        </ul>
      ) : null}
    </section>
  );
}
