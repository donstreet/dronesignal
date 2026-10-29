import unittest
from classifier import parse_post

class ClassifierTests(unittest.TestCase):
    def test_water_delivery(self):
        post = parse_post("Подвоз воды Макеевка 29.10 по адресам", "https://t.me/vodavdnr/1")
        self.assertEqual(post.category, "water")
        self.assertEqual(post.kind, "delivery")
        self.assertFalse(post.requires_review)

    def test_common_light_wording(self):
        post = parse_post("Макеевка: график отключения света 29.10", "https://t.me/Novosti_Makeevka/42")
        self.assertEqual(post.category, "electricity")
        self.assertFalse(post.requires_review)

    def test_missing_city_needs_review(self):
        post = parse_post("График отключения электроэнергии 29.10", "https://max.ru/example")
        self.assertTrue(post.requires_review)

    def test_fuel_is_not_alert(self):
        post = parse_post("АЗС бензин дизель", "https://t.me/fueldnr/4161")
        self.assertEqual(post.category, "fuel")
        self.assertTrue(post.requires_review)

    def test_municipal_city_aliases_are_detected_but_news_stays_in_review(self):
        post = parse_post("Администрация Горловки сообщает о ремонте дороги", "https://t.me/Gorlowka_now/12")
        self.assertEqual(post.category, "news")
        self.assertEqual(post.cities, ("Горловка",))
        self.assertTrue(post.requires_review)

    def test_two_cities_need_review(self):
        post = parse_post("Макеевка и Донецк график отключения света 29.10", "https://t.me/x/1")
        self.assertTrue(post.requires_review)

if __name__ == "__main__":
    unittest.main()
