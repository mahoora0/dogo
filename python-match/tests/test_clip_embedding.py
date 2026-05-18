import os

from app.clip_embedding import ANIMAL_CROP, CLIP_MODEL_NAME, CLIP_PROCESSOR_NAME, _expand_to_square


def assert_square_inside_image(crop: tuple[int, int, int, int], image_size: tuple[int, int]):
    left, top, right, bottom = crop
    width, height = image_size

    assert 0 <= left < right <= width
    assert 0 <= top < bottom <= height
    assert right - left == bottom - top


def test_animal_crop_type_is_versioned():
    assert ANIMAL_CROP == "ANIMAL_CROP_V2"


def test_default_pet_embedding_model_is_zer0int_clip_l():
    assert CLIP_MODEL_NAME == os.getenv(
        "CLIP_MODEL_NAME",
        "AvitoTech/Zer0int-CLIP-L-for-animal-identification",
    )
    assert CLIP_PROCESSOR_NAME == os.getenv("CLIP_PROCESSOR_NAME", "zer0int/CLIP-GmP-ViT-L-14")


def test_expand_to_square_keeps_center_bbox_square():
    image_size = (200, 200)
    crop = _expand_to_square((50, 60, 130, 140), image_size)

    assert_square_inside_image(crop, image_size)


def test_expand_to_square_shifts_from_left_edge():
    image_size = (200, 200)
    crop = _expand_to_square((0, 50, 80, 150), image_size)

    assert crop[0] == 0
    assert_square_inside_image(crop, image_size)


def test_expand_to_square_shifts_from_right_edge():
    image_size = (200, 200)
    crop = _expand_to_square((120, 50, 200, 150), image_size)

    assert crop[2] == image_size[0]
    assert_square_inside_image(crop, image_size)


def test_expand_to_square_shifts_from_top_edge():
    image_size = (200, 200)
    crop = _expand_to_square((50, 0, 150, 80), image_size)

    assert crop[1] == 0
    assert_square_inside_image(crop, image_size)


def test_expand_to_square_shifts_from_bottom_edge():
    image_size = (200, 200)
    crop = _expand_to_square((50, 120, 150, 200), image_size)

    assert crop[3] == image_size[1]
    assert_square_inside_image(crop, image_size)


def test_expand_to_square_caps_side_to_shorter_image_edge():
    image_size = (120, 80)
    crop = _expand_to_square((10, 10, 110, 70), image_size)

    assert crop[2] - crop[0] == 80
    assert_square_inside_image(crop, image_size)
