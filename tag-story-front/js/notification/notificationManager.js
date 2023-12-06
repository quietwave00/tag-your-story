const authorizationToken = localStorage.getItem("Authorization");

export const eventSource = authorizationToken
  ? new EventSource(`${server_host}/api/notification/subscription?AccessToken=${authorizationToken}`)
  : null;