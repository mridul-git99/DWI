export const getVideoDevices = async () => {
  const allDevices = await navigator.mediaDevices.enumerateDevices();
  const videoDevices = allDevices.filter(({ kind }) => kind === 'videoinput');
  return videoDevices || [];
};
