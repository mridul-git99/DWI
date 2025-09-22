export const getSummary = (duration: Record<string, number>) => {
  const { day, hour, minute } = duration;
  let summary = '';
  if (day > 0) {
    summary += ` ${day} Day${day > 1 ? 's' : ''}`;
  }

  if (hour > 0) {
    summary += ` ${hour} Hour${hour > 1 ? 's' : ''}`;
  }

  if (minute > 0) {
    summary += ` ${minute} Minute${minute > 1 ? 's' : ''}`;
  }

  return summary;
};
