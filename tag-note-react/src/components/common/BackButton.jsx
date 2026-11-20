import { useTranslation } from 'react-i18next';

export default function BackButton({ label, onClick }) {
  const { t } = useTranslation('tracks');
  return (
    <button aria-label={label ?? t('detail.back')} className="detail-back-button" onClick={onClick} type="button">
      <span aria-hidden="true">◀</span>
    </button>
  );
}
