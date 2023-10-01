const deleteCookie = (cookieName) => {
    const cookies = document.cookie.split('; ');

    for (let i = 0; i < cookies.length; i++) {
        const cookie = cookies[i];
        const cookieParts = cookie.split('=');
        const name = cookieParts[0];

        if (name === cookieName) {
            document.cookie = name + '=; expires=Thu, 01 Jan 1970 00:00:00 UTC; path=/;';
            return;
        }
    }
}

export default {
    deleteCookie
}