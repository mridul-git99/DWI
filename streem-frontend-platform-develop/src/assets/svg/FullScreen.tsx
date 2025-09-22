import React from 'react';

function FullScreen() {
  return (
    <svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" fill="none" viewBox="0 0 24 24">
      <path
        fill="#fff"
        fillOpacity="0.4"
        d="M0 12C0 5.373 5.373 0 12 0s12 5.373 12 12-5.373 12-12 12S0 18.627 0 12z"
      ></path>
      <path
        fill="#fff"
        d="M14.333 5l1.79 1.789-2.249 2.232 1.105 1.105 2.232-2.248L19 9.667V5h-4.667zM5 9.667l1.789-1.79 2.232 2.249 1.105-1.105L7.878 6.79 9.667 5H5v4.667zM9.667 19l-1.79-1.789 2.249-2.232-1.105-1.105-2.232 2.248L5 14.333V19h4.667zM19 14.333l-1.789 1.79-2.232-2.249-1.105 1.105 2.248 2.232L14.333 19H19v-4.667z"
      ></path>
    </svg>
  );
}

export default FullScreen;
