import { useTranslation } from 'react-i18next';
import '../styles/contact.css';

const contactLinks = [
  { label: 'GITHUB', href: 'https://github.com/' },
  { label: 'SPOTIFY', href: 'https://open.spotify.com/' },
  { label: 'INSTAGRAM', href: 'https://www.instagram.com/' },
  { label: 'EMAIL', href: 'mailto:hello@tagnote.dev' },
];

export default function ContactPage() {
  const { t } = useTranslation('common');

  return (
    <section className="contact-page" aria-labelledby="contact-title">
      <h1 className="contact-title" id="contact-title">
        {t('contact.title')}
      </h1>
      <nav className="contact-links" aria-label={t('contact.linksLabel')}>
        {contactLinks.map((link) => (
          <a className="contact-link" href={link.href} key={link.label} rel="noreferrer" target="_blank">
            <span className="contact-icon" aria-hidden="true" />
            <span>{link.label}</span>
          </a>
        ))}
      </nav>
    </section>
  );
}
