import { isRouteErrorResponse, useRouteError } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import EmptyState from '../components/common/EmptyState.jsx';

export default function ExceptionPage() {
  const { t } = useTranslation('common');
  const error = useRouteError();
  const message = isRouteErrorResponse(error)
    ? `${error.status} ${error.statusText}`
    : t('errors.generic');

  return <EmptyState title={t('errors.title')} description={message} />;
}
