import { isRouteErrorResponse, useRouteError } from 'react-router-dom';
import EmptyState from '../components/common/EmptyState.jsx';

export default function ExceptionPage() {
  const error = useRouteError();
  const message = isRouteErrorResponse(error)
    ? `${error.status} ${error.statusText}`
    : '요청을 처리하지 못했습니다.';

  return <EmptyState title="오류가 발생했습니다" description={message} />;
}
